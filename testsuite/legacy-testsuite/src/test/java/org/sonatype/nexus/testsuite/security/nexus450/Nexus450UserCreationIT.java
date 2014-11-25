/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.testsuite.security.nexus450;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.EmailServerHelper;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.nexus.test.utils.UserCreationUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.nexus.testsuite.security.ChangePasswordUtils;
import org.sonatype.security.rest.model.UserResource;

import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

/**
 * Using admin account create a new user. Then check for new user creation confirmation e-mail and password. Login and
 * change password. Confirm if it can login.
 */
public class Nexus450UserCreationIT
    extends AbstractNexusIntegrationTest
{
  @ClassRule
  public static final EmailServerHelper emailServerHelper = new EmailServerHelper();

  private UserMessageUtil userUtil;

  private static final String USER_ID = "velo";

  @BeforeClass
  public static void enableSecureContext() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void init() {
    userUtil =
        new UserMessageUtil(this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void createUser()
      throws Exception
  {
    TestContext testContext = TestContainer.getInstance().getTestContext();
    testContext.useAdminForRequests();

    // create user,
    UserResource resource = new UserResource();
    resource.setUserId(USER_ID);
    resource.setFirstName("Marvin Velo");
    resource.setEmail("velo@earth.com");
    resource.setStatus("active");
    resource.addRole("nx-admin");
    userUtil.createUser(resource);

    // get email
    // two e-mails (first confirming user creating and second with users pw)
    emailServerHelper.waitForMail(2);
    Thread.sleep(1000); //w8 a few more

    MimeMessage[] msgs = emailServerHelper.getReceivedMessages();
    String password = null;
    StringBuilder emailsContent = new StringBuilder();

    /// make sure we have at least 1 message
    Assert.assertTrue("No emails recieved.", msgs.length > 0);

    for (MimeMessage mimeMessage : msgs) {

      boolean toVelo = false;
      Address[] recipients = mimeMessage.getAllRecipients();
      for (Address address : recipients) {
        InternetAddress addr = (InternetAddress) address;
        if ("velo@earth.com".equals(addr.getAddress())) {
          toVelo = true;
        }
      }
      if (!toVelo) {
        continue;
      }

      emailsContent.append(GreenMailUtil.getHeaders(mimeMessage)).append('\n');

      // Sample body: Your new password is ********
      String body = GreenMailUtil.getBody(mimeMessage);
      emailsContent.append(body).append('\n').append('\n');

      int index = body.indexOf("Your new password is ");
      int passwordStartIndex = index + "Your new password is ".length();
      if (index != -1) {
        password = body.substring(passwordStartIndex, body.indexOf('\n', passwordStartIndex)).trim();
        log.debug("New password:\n" + password);
        break;
      }
    }

    Assert.assertNotNull(password, "Didn't recieve a password.  Got the following messages:\n" + emailsContent);

    // login with generated password
    testContext.setUsername(USER_ID);
    testContext.setPassword(password);
    Status status = UserCreationUtil.login();
    Assert.assertTrue(status.isSuccess());

    // set new password
    String newPassword = "velo123";
    status = ChangePasswordUtils.changePassword(USER_ID, password, newPassword);
    Assert.assertTrue(status.isSuccess());

    // check if the user is 'active'
    testContext.useAdminForRequests();
    UserResource user = userUtil.getUser(USER_ID);
    Assert.assertEquals(user.getStatus(), "active");

    // login with new password
    testContext.setUsername(USER_ID);
    testContext.setPassword(newPassword);
    status = UserCreationUtil.login();
    Assert.assertTrue(status.isSuccess());
  }

  @After
  public void removeUser()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    UserMessageUtil.removeUser(USER_ID);
  }

}
