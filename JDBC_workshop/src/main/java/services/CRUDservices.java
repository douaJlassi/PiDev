package services;

import java.sql.SQLException;
import java.util.List;
public interface CRUDservices<T,U> {
    void insertOne(U u) throws SQLException;
    void updateOne(T t,U u) throws SQLException;
    void deleteOne(U u) throws SQLException;
    List<U> selectALL() throws SQLException;
}
