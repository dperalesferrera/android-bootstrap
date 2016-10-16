package com.donnfelker.android.bootstrap.ui.nosql;

import android.util.Log;
import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.FavoriteNewsDO;
import com.donnfelker.android.bootstrap.core.News;

public class NoSQLTableFavoriteNews extends NoSQLTableBase {
    private static final String LOG_TAG = NoSQLTableFavoriteNews.class.getSimpleName();

    /** The DynamoDB object mapper for accessing DynamoDB. */
    private final DynamoDBMapper mapper;

    public NoSQLTableFavoriteNews() {
        mapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
    }

    @Override
    public String getTableName() {
        return "favorite_news";
    }

    @Override
    public String getPartitionKeyName() {
        return "userId";
    }

    public String getPartitionKeyType() {
        return "String";
    }

    @Override
    public String getSortKeyName() {
        return "objectid";
    }

    public String getSortKeyType() {
        return "String";
    }

    @Override
    public int getNumIndexes() {
        return 0;
    }


    public void insertData(final News newsItem) throws AmazonClientException {
        Log.d(LOG_TAG, "Inserting new viewed data to table.");
        final FavoriteNewsDO firstItem = new FavoriteNewsDO();

        firstItem.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
        firstItem.setObjectid(newsItem.getObjectId());
        firstItem.setContent(newsItem.getContent());
        firstItem.setTitle(newsItem.getTitle());
        AmazonClientException lastException = null;

        try {
            mapper.save(firstItem);
        } catch (final AmazonClientException ex) {
            Log.e(LOG_TAG, "Failed saving item : " + ex.getMessage(), ex);
            lastException = ex;
        }

        if (lastException != null) {
            // Re-throw the last exception encountered to alert the user.
            throw lastException;
        }
    }


}
