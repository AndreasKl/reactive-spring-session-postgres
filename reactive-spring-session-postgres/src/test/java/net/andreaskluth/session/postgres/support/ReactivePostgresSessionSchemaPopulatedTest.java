package net.andreaskluth.session.postgres.support;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.opentable.db.postgres.junit5.EmbeddedPostgresExtension;
import com.opentable.db.postgres.junit5.PreparedDbExtension;
import io.vertx.pgclient.PgException;
import io.vertx.pgclient.PgPool;
import net.andreaskluth.session.postgres.TestPostgresOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Mono;

class ReactivePostgresSessionSchemaPopulatedTest {

  @RegisterExtension
  static final PreparedDbExtension embeddedPostgres =
      EmbeddedPostgresExtension.preparedDatabase(ds -> {});

  private static final String[] DEFECTIVE_SCHEMA = {
    "CREATE TABLE demo (id text);", "CREATE TABLE demo (id text);"
  };

  @Test
  void schemaIsCreated() {
    ReactiveSessionSchemaPopulator.applyDefaultSchema(pool()).block();

    Mono.create(
            sink ->
                pool()
                    .query(
                        "SELECT * FROM session",
                        event -> {
                          if (event.succeeded()) {
                            sink.success();
                            return;
                          }
                          sink.error(event.cause());
                        }))
        .block();
  }

  @Test
  void schemaCanBeAppliedMultipleTimes() {
    ReactiveSessionSchemaPopulator.applyDefaultSchema(pool()).block();
    ReactiveSessionSchemaPopulator.applyDefaultSchema(pool()).block();
    ReactiveSessionSchemaPopulator.applyDefaultSchema(pool()).block();

    Mono.create(
            sink ->
                pool()
                    .query(
                        "SELECT * FROM session",
                        event -> {
                          if (event.succeeded()) {
                            sink.success();
                            return;
                          }
                          sink.error(event.cause());
                        }))
        .block();
  }

  @Test
  void failsIfStatementsCanNotBeExecuted() {
    assertThatThrownBy(
            () ->
                ReactiveSessionSchemaPopulator.applySchema(pool(), DEFECTIVE_SCHEMA)
                    .block())
        .isInstanceOf(PgException.class);
  }

  private PgPool pool() {
    return TestPostgresOptions.pool(embeddedPostgres.getConnectionInfo().getPort());
  }
}
