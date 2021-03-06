package io.trane.ndbc.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Test;

import io.trane.future.CheckedFutureException;
import io.trane.future.Future;
import io.trane.ndbc.DataSource;
import io.trane.ndbc.PreparedStatement;
import io.trane.ndbc.Row;

public class PooledDataSourceTest extends PoolEnv {

  String    query     = "SELECT 1";
  String    statement = "DELETE FROM T";
  Duration  timeout   = Duration.ofSeconds(1);
  List<Row> rows      = new ArrayList<>();

  @Test
  public void query() throws CheckedFutureException {
    final Connection c = new TestConnection() {
      @Override
      public Future<List<Row>> query(final String query) {
        assertEquals(PooledDataSourceTest.this.query, query);
        return Future.value(rows);
      }
    };

    assertEquals(rows, ds(c).query(query).get(timeout));
  }

  @Test
  public void execute() throws CheckedFutureException {
    final Long result = 121L;
    final Connection c = new TestConnection() {
      @Override
      public Future<Long> execute(final String statement) {
        assertEquals(PooledDataSourceTest.this.statement, statement);
        return Future.value(result);
      }
    };

    assertEquals(result, ds(c).execute(statement).get(timeout));
  }

  @Test
  public void queryPreparedStatement() throws CheckedFutureException {
    final PreparedStatement ps = PreparedStatement.apply(query);
    final List<Row> rows = new ArrayList<>();
    final Connection c = new TestConnection() {
      @Override
      public Future<List<Row>> query(final PreparedStatement query) {
        assertEquals(ps, query);
        return Future.value(rows);
      }
    };

    assertEquals(rows, ds(c).query(ps).get(timeout));
  }

  @Test
  public void executePreparedStatement() throws CheckedFutureException {
    final PreparedStatement ps = PreparedStatement.apply(statement);
    final Long result = 121L;
    final Connection c = new TestConnection() {
      @Override
      public Future<Long> execute(final PreparedStatement statement) {
        assertEquals(ps, statement);
        return Future.value(result);
      }
    };

    assertEquals(result, ds(c).execute(ps).get(timeout));
  }

  @Test
  public void transactional() throws CheckedFutureException {
    final Integer result = 1;
    final Supplier<Future<Integer>> block = () -> Future.value(result);
    final Connection c = new TestConnection() {
      @Override
      public <R> Future<R> withTransaction(final Supplier<Future<R>> sup) {
        assertEquals(block, sup);
        return sup.get();
      }
    };
    assertEquals(result, ds(c).transactional(block).get(timeout));
  }

  @Test
  public void transactionalFailedSupplierDoesntThrow() throws CheckedFutureException {
    final Supplier<Future<Integer>> block = () -> {
      throw new RuntimeException();
    };
    final Connection c = new TestConnection() {
      @Override
      public <R> Future<R> withTransaction(final Supplier<Future<R>> sup) {
        assertEquals(block, sup);
        return sup.get();
      }
    };
    ds(c).transactional(block);
  }

  @Test
  public void transactionalNested() throws CheckedFutureException {
    final Integer result = 1;
    final AtomicBoolean called = new AtomicBoolean(false);
    final Connection c = new TestConnection() {
      @Override
      public <R> Future<R> withTransaction(final Supplier<Future<R>> sup) {
        assertFalse(called.get());
        called.set(true);
        return sup.get();
      }
    };
    final DataSource ds = ds(c);
    final Supplier<Future<Integer>> block = () -> ds.transactional(() -> Future.value(result));
    assertEquals(result, ds.transactional(block).get(timeout));
  }

  @Test
  public void transactionalQuery() throws CheckedFutureException {
    final AtomicBoolean called = new AtomicBoolean(false);
    final Connection c = new TestConnection() {
      @Override
      public Future<List<Row>> query(final String query) {
        assertEquals(PooledDataSourceTest.this.query, query);
        return Future.value(rows);
      }

      @Override
      public <R> Future<R> withTransaction(final Supplier<Future<R>> sup) {
        assertFalse(called.get());
        called.set(true);
        return sup.get();
      }
    };
    final DataSource ds = ds(c);
    final Supplier<Future<List<Row>>> block = () -> ds.transactional(() -> ds.query(query));
    assertEquals(rows, ds.transactional(block).get(timeout));
  }

  @Test
  public void close() throws CheckedFutureException {
    ds(new TestConnection()).close().get(timeout);
  }

  private DataSource ds(final Connection c) {
    final Pool<Connection> pool = LockFreePool.apply(() -> Future.value(c), Optional.empty(),
        Optional.empty(),
        Optional.empty(), scheduler);
    return new PooledDataSource(pool);
  }
}
