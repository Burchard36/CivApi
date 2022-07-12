package org.example.utils.database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.Document;

public class DatabaseResult {

    protected Document resultData;
    protected DatabaseActionType type;
    protected Boolean booleanResult;

    public DatabaseResult(@Nonnull DatabaseActionType actionType,
                          @Nullable Boolean booleanResult,
                          @Nullable Document resultData) {
        this.type = actionType;
        this.booleanResult = booleanResult;
        this.resultData = resultData;
    }

    public DatabaseActionType getType() {
        return this.type;
    }

    public Document getData() {
        return this.resultData;
    }

    public Boolean getBooleanResult() {
        return this.booleanResult;
    }
}

