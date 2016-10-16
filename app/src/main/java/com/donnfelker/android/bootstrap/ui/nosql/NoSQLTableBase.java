package com.donnfelker.android.bootstrap.ui.nosql;

import com.donnfelker.android.bootstrap.core.News;

public abstract class NoSQLTableBase {
    /** The number of sample data entries to be inserted when calling insertSampleData. */
    public static final int SAMPLE_DATA_ENTRIES_PER_INSERT = 20;

    /**
     * @return the name of the table.
     */
    public abstract String getTableName();

    /**
     * @return the primary partition key for the table.
     */
    public abstract String getPartitionKeyName();

    /**
     * @return the human readable partition key type.
     */
    public abstract String getPartitionKeyType();
    /**
     * @return the secondary partition key for the table.
     */
    public abstract String getSortKeyName();

    /**
     * @return the human readable sort key type.
     */
    public abstract String getSortKeyType();

    /**
     * @return the number of secondary indexes for the table.
     */
    public abstract int getNumIndexes();

    /**
     * Insert news data into the table.
     */
    public abstract void insertData(News newsItem);

}
