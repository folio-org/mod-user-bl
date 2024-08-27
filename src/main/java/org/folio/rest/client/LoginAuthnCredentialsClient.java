package org.folio.rest.client;

import io.vertx.core.Future;
import org.folio.rest.util.OkapiConnectionParams;

public interface LoginAuthnCredentialsClient {

  /**
   * Delete user's auth credentials by userId
   * @param userId for which we want to delete auth credentials
   * @param connectionParams okapi metadata
   * @return Returns <b>true</b> if the record has been deleted, <br/> otherwise <b>false</b> if no record for the
   * userId exists,
   * <br/> <b>OkapiModuleClientException</b> exception if any exception occurred
   */
  Future<Boolean> deleteAuthnCredentialsByUserId(String userId, OkapiConnectionParams connectionParams);
}