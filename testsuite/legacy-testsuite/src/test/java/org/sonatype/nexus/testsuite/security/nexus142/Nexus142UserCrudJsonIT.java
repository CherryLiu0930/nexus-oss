/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.testsuite.security.nexus142;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.UserResource;

import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * CRUD tests for JSON request/response.
 */
public class Nexus142UserCrudJsonIT
    extends AbstractNexusIntegrationTest
{
  protected UserMessageUtil messageUtil;

  private PasswordService passwordService;

  public Nexus142UserCrudJsonIT() {
    this.messageUtil = new UserMessageUtil(this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Before
  public void setUp()
      throws Exception
  {
    passwordService = TestContainer.getInstance().getPlexusContainer().lookup(PasswordService.class);
  }

  @Test
  public void createUserTest()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("Create User");
    resource.setUserId("createUser");
    resource.setStatus("active");
    resource.setEmail("nexus@user.com");
    resource.addRole("role1");

    // this also validates
    this.messageUtil.createUser(resource);
  }

  @Test
  public void listTest()
      throws IOException
  {
    UserResource resource = new UserResource();

    resource.setFirstName("list Test");
    resource.setUserId("listTest");
    resource.setStatus("active");
    resource.setEmail("listTest@user.com");
    resource.addRole("role1");

    // this also validates
    this.messageUtil.createUser(resource);

    List<UserResource> users = this.messageUtil.getList();
    UserResource foundUser = null;
    for (UserResource user : users) {
      if (resource.getUserId().equals(user.getUserId())) {
        foundUser = user;
        break;
      }
    }
    assertThat(foundUser, is(notNullValue()));
    assertThat(resource.getFirstName(), equalTo(foundUser.getFirstName()));
    assertThat(resource.getLastName(), equalTo(foundUser.getLastName()));
    assertThat(resource.getEmail(), equalTo(foundUser.getEmail()));
    assertThat(resource.getStatus(), equalTo(foundUser.getStatus()));
    assertThat(resource.getRoles(), containsInAnyOrder("role1"));
  }

  public void readTest()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("Read User");
    resource.setUserId("readUser");
    resource.setStatus("active");
    resource.setEmail("read@user.com");
    resource.addRole("role1");

    // this also validates
    this.messageUtil.createUser(resource);

    Response response = this.messageUtil.sendMessage(Method.GET, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not GET Repository Target: " + response.getStatus());
    }

    // get the Resource object
    UserResource responseResource = this.messageUtil.getResourceFromResponse(response);

    Assert.assertEquals(responseResource.getFirstName(), resource.getFirstName());
    Assert.assertEquals(responseResource.getUserId(), resource.getUserId());
    Assert.assertEquals(responseResource.getStatus(), "active");
    Assert.assertEquals(responseResource.getEmail(), resource.getEmail());
    Assert.assertEquals(resource.getRoles(), responseResource.getRoles());
  }

  @Test
  public void updateTest()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("Update User");
    resource.setUserId("updateUser");
    resource.setStatus("active");
    resource.setEmail("updateUser@user.com");
    resource.addRole("role1");

    this.messageUtil.createUser(resource);

    // update the user
    // TODO: add tests that changes the userId
    resource.setFirstName("Update UserAgain");
    resource.setUserId("updateUser");
    resource.setStatus("active");
    resource.setEmail("updateUser@user2.com");
    resource.getRoles().clear();
    resource.addRole("role2");

    // this validates
    this.messageUtil.updateUser(resource);

  }

  @Test
  public void deleteTest()
      throws IOException
  {

    UserResource resource = new UserResource();

    resource.setFirstName("Delete User");
    resource.setUserId("deleteUser");
    resource.setStatus("active");
    resource.setEmail("deleteUser@user.com");
    resource.addRole("role2");

    this.messageUtil.createUser(resource);

    // use the new ID
    Response response = this.messageUtil.sendMessage(Method.DELETE, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not delete User: " + response.getStatus());
    }
  }

}
