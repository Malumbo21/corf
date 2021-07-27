package org.telekit.base.util.jdbc;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MapperBasedListHandler<T> extends AbstractListHandler<T> {

    protected final ResultSetMapper<T> mapper;

    public MapperBasedListHandler(ResultSetMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected T handleRow(ResultSet resultSet) throws SQLException {
        return mapper.map(resultSet);
    }
}