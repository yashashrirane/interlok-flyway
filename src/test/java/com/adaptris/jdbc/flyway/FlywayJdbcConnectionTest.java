package com.adaptris.jdbc.flyway;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.adaptris.core.jdbc.DatabaseConnectionCase;
import com.adaptris.core.util.LifecycleHelper;
import com.adaptris.util.TimeInterval;

@SuppressWarnings("deprecation")
public class FlywayJdbcConnectionTest extends DatabaseConnectionCase<FlywayJdbcConnection> {

  public FlywayJdbcConnectionTest(String arg0) {
    super(arg0);
  }

  @Test
  public void testGetFlywayLocations(){
    FlywayJdbcConnection connection = new FlywayJdbcConnection();
    connection.setFlywayLocations(Collections.singletonList("classpath:migration"));
    assertEquals(1, connection.getFlywayLocations().size());
    assertEquals("classpath:migration", connection.getFlywayLocations().get(0));
  }

  @Test
  public void testGetBaseline() throws Exception {
    FlywayJdbcConnection connection = new FlywayJdbcConnection();
    assertNull(connection.getBaseline());
    connection.setBaseline(true);
    assertTrue(connection.getBaseline());
  }


  @Test
  public void testGetFlyway() throws Exception {
    FlywayJdbcConnection connection = new FlywayJdbcConnection();
    assertNull(connection.getFlywayLocations());
    assertNull(connection.getFlyway());
    // Should return us the dumb functional interface
    assertNotNull(connection.migrator());
    // which means I can do this.
    connection.migrator().migrate(null);
    FlywayMigrator migrator = new DefaultFlywayMigrator().withBaseline(true).withFlywayLocations("classpath:migration/partial");
    connection.setFlyway(migrator);
    assertSame(migrator, connection.getFlyway());
    assertSame(migrator, connection.migrator());
  }

  @Test
  public void testConnectionWhenInitialisedFullMigration_Legacy() throws Exception {
    FlywayJdbcConnection con = configure(createConnection(), initialiseFlywayDatabase());
    try {
      con.setBaseline(false);
      con.setFlywayLocations(Collections.singletonList("classpath:migration/full"));
      LifecycleHelper.init(con);
      con.connect();
      FlywayMigratorTest.verifyCount(1, FlywayMigratorTest.connection(con.getConnectUrl()));
    } finally {
      LifecycleHelper.stopAndClose(con);
    }
  }

  @Test
  public void testConnectionWhenInitialisedFullMigration() throws Exception {
    FlywayJdbcConnection con = configure(createConnection(), initialiseFlywayDatabase());
    try {
      con.setFlywayLocations(null);
      con.setFlyway(new DefaultFlywayMigrator().withFlywayLocations(Collections.singletonList("classpath:migration/full")));
      LifecycleHelper.init(con);
      con.connect();
      FlywayMigratorTest.verifyCount(1, FlywayMigratorTest.connection(con.getConnectUrl()));
    } finally {
      LifecycleHelper.stopAndClose(con);
    }
  }

  @Override
  protected FlywayJdbcConnection createConnection() {
    return new FlywayJdbcConnection();
  }

  @Override
  protected FlywayJdbcConnection configure(FlywayJdbcConnection flywayJdbcConnection) throws Exception {
    String url = initialiseDatabase();
    return configure(flywayJdbcConnection, url);
  }

  private FlywayJdbcConnection configure(FlywayJdbcConnection flywayJdbcConnection, String url) throws Exception {
    flywayJdbcConnection.setConnectUrl(url);
    flywayJdbcConnection.setDriverImp(DRIVER_IMP);
    flywayJdbcConnection.setTestStatement(DEFAULT_TEST_STATEMENT);
    flywayJdbcConnection.setDebugMode(true);
    flywayJdbcConnection.setConnectionAttempts(1);
    flywayJdbcConnection.setConnectionRetryInterval(new TimeInterval(10L, TimeUnit.MILLISECONDS.name()));
    flywayJdbcConnection.setAlwaysValidateConnection(false);
    // The always-validate tests require a database, so we need to baseline
    return flywayJdbcConnection
        .withFlyway(new DefaultFlywayMigrator().withBaseline(true).withFlywayLocations("classpath:migration/partial"));
  }

  protected String initialiseFlywayDatabase() throws Exception {
    return "jdbc:derby:memory:" + nameGen.safeUUID() + ";create=true";
  }
}