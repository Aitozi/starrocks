// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Limited.

package com.starrocks.sql.ast;

import com.google.common.collect.Lists;
import com.starrocks.analysis.RedirectStatus;
import com.starrocks.analysis.ShowStmt;
import com.starrocks.catalog.Column;
import com.starrocks.catalog.Database;
import com.starrocks.catalog.ScalarType;
import com.starrocks.catalog.Table;
import com.starrocks.cluster.ClusterNamespace;
import com.starrocks.common.MetaNotFoundException;
import com.starrocks.qe.ShowResultSetMetaData;
import com.starrocks.server.GlobalStateMgr;
import com.starrocks.statistic.AnalyzeJob;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ShowAnalyzeJobStmt extends ShowStmt {

    private static final ShowResultSetMetaData META_DATA =
            ShowResultSetMetaData.builder()
                    .addColumn(new Column("Id", ScalarType.createVarchar(60)))
                    .addColumn(new Column("Database", ScalarType.createVarchar(60)))
                    .addColumn(new Column("Table", ScalarType.createVarchar(60)))
                    .addColumn(new Column("Columns", ScalarType.createVarchar(200)))
                    .addColumn(new Column("Type", ScalarType.createVarchar(20)))
                    .addColumn(new Column("Schedule", ScalarType.createVarchar(20)))
                    .addColumn(new Column("Properties", ScalarType.createVarchar(200)))
                    .addColumn(new Column("Status", ScalarType.createVarchar(20)))
                    .addColumn(new Column("LastWorkTime", ScalarType.createVarchar(60)))
                    .addColumn(new Column("Reason", ScalarType.createVarchar(100)))
                    .build();

    public static List<String> showAnalyzeJobs(AnalyzeJob analyzeJob) throws MetaNotFoundException {
        List<String> row = Lists.newArrayList("", "ALL", "ALL", "ALL", "", "", "", "", "", "");
        long dbId = analyzeJob.getDbId();
        long tableId = analyzeJob.getTableId();
        List<String> columns = analyzeJob.getColumns();

        row.set(0, String.valueOf(analyzeJob.getId()));
        if (AnalyzeJob.DEFAULT_ALL_ID != dbId) {
            Database db = GlobalStateMgr.getCurrentState().getDb(dbId);

            if (db == null) {
                throw new MetaNotFoundException("No found database: " + dbId);
            }

            row.set(1, ClusterNamespace.getNameFromFullName(db.getFullName()));

            if (AnalyzeJob.DEFAULT_ALL_ID != tableId) {
                Table table = db.getTable(tableId);

                if (table == null) {
                    throw new MetaNotFoundException("No found table: " + tableId);
                }

                row.set(2, table.getName());

                if (null != columns && !columns.isEmpty()
                        && (columns.size() != table.getBaseSchema().size())) {
                    String str = String.join(",", columns);
                    if (str.length() > 100) {
                        row.set(3, str.substring(0, 100) + "...");
                    } else {
                        row.set(3, str);
                    }
                }
            }
        }

        row.set(4, analyzeJob.getType().name());
        row.set(5, analyzeJob.getScheduleType().name());
        row.set(6, analyzeJob.getProperties() == null ? "{}" : analyzeJob.getProperties().toString());
        row.set(7, analyzeJob.getStatus().name());
        if (LocalDateTime.MIN.equals(analyzeJob.getWorkTime())) {
            row.set(8, "None");
        } else {
            row.set(8, analyzeJob.getWorkTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (null != analyzeJob.getReason()) {
            row.set(9, analyzeJob.getReason());
        }

        return row;
    }

    @Override
    public ShowResultSetMetaData getMetaData() {
        return META_DATA;
    }

    @Override
    public RedirectStatus getRedirectStatus() {
        return RedirectStatus.FORWARD_NO_SYNC;
    }

    @Override
    public boolean isSupportNewPlanner() {
        return true;
    }
}
