package br.com.economiamod.server.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

final class SqliteJdbcCompatibility {
    private SqliteJdbcCompatibility() {
    }

    static Connection wrap(Connection delegate, DatabaseEngine engine) {
        if (engine != DatabaseEngine.SQLITE) {
            return delegate;
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionHandler(delegate)
        );
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection delegate;

        private ConnectionHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (("prepareStatement".equals(name) || "prepareCall".equals(name)) && args != null && args.length > 0 && args[0] instanceof String sql) {
                Object[] adapted = args.clone();
                adapted[0] = SqlDialect.adapt(sql, DatabaseEngine.SQLITE);
                Object statement = method.invoke(delegate, adapted);
                return statement instanceof PreparedStatement prepared ? wrapPrepared(prepared) : statement;
            }
            if ("createStatement".equals(name)) {
                return wrapStatement((Statement) method.invoke(delegate, args));
            }
            if ("unwrap".equals(name) && args != null && args.length == 1 && args[0] instanceof Class<?> target && target.isInstance(delegate)) {
                return delegate;
            }
            if ("isWrapperFor".equals(name) && args != null && args.length == 1 && args[0] instanceof Class<?> target && target.isInstance(delegate)) {
                return true;
            }
            return invokeDelegate(delegate, method, args);
        }
    }

    private static Statement wrapStatement(Statement delegate) {
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(), new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    if (args != null && args.length > 0 && args[0] instanceof String sql && method.getName().startsWith("execute")) {
                        Object[] adapted = args.clone();
                        adapted[0] = SqlDialect.adapt(sql, DatabaseEngine.SQLITE);
                        Object result = invokeDelegate(delegate, method, adapted);
                        return wrapResultIfNeeded(result);
                    }
                    return wrapResultIfNeeded(invokeDelegate(delegate, method, args));
                });
    }

    private static PreparedStatement wrapPrepared(PreparedStatement delegate) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setObject".equals(method.getName()) && args != null && args.length >= 2) {
                        Object value = args[1];
                        if (value instanceof UUID uuid) {
                            delegate.setString((Integer) args[0], uuid.toString());
                            return null;
                        }
                        if (value instanceof LocalDate date) {
                            delegate.setString((Integer) args[0], date.toString());
                            return null;
                        }
                        if (value instanceof LocalDateTime dateTime) {
                            delegate.setString((Integer) args[0], dateTime.toString());
                            return null;
                        }
                    }
                    if ("setNull".equals(method.getName()) && args != null && args.length >= 2
                            && args[1] instanceof Integer sqlType && sqlType == Types.OTHER) {
                        delegate.setNull((Integer) args[0], Types.VARCHAR);
                        return null;
                    }
                    return wrapResultIfNeeded(invokeDelegate(delegate, method, args));
                });
    }

    private static ResultSet wrapResult(ResultSet delegate) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getObject".equals(method.getName()) && args != null && args.length == 2 && args[1] instanceof Class<?> target) {
                        if (target == UUID.class) {
                            String value = args[0] instanceof String label ? delegate.getString(label) : delegate.getString((Integer) args[0]);
                            return value == null || value.isBlank() ? null : UUID.fromString(value);
                        }
                        if (target == LocalDate.class) {
                            String value = args[0] instanceof String label ? delegate.getString(label) : delegate.getString((Integer) args[0]);
                            return value == null || value.isBlank() ? null : LocalDate.parse(value.substring(0, Math.min(10, value.length())));
                        }
                        if (target == LocalDateTime.class) {
                            String value = args[0] instanceof String label ? delegate.getString(label) : delegate.getString((Integer) args[0]);
                            if (value == null || value.isBlank()) {
                                return null;
                            }
                            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
                        }
                    }
                    return invokeDelegate(delegate, method, args);
                });
    }

    private static Object wrapResultIfNeeded(Object result) {
        return result instanceof ResultSet resultSet ? wrapResult(resultSet) : result;
    }

    private static Object invokeDelegate(Object delegate, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
