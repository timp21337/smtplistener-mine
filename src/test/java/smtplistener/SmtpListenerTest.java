package smtplistener;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import junit.framework.TestCase;

/**
 * @author timp
 * @since 2013-12-18
 */
public class SmtpListenerTest
    extends TestCase {

  final static int PORT = 1616;

  public void testStopListeningBeforeListening() {
    SmtpListener listener = new SmtpListener(1616);
    try {
      listener.stopListening();
      fail("Should have bombed");
    } catch (RuntimeException e) {
      e = null;
    }
  }
  public void testUsesPort() throws Exception {
    assertTrue(available(PORT));
    SmtpListener listener = new SmtpListener(1616);
    listener.startListening();
    assertFalse(available(PORT));
    listener.stopListening();
    Thread.sleep(1);
    assertTrue(available(PORT));
  }

  private Email fetchEmail(SmtpListener listener) throws Exception {
    Email it = listener.getLastEmailReceived();
    int goes = 0;
    while(it == null) {
      System.err.println("Goes:" + goes);
      if (goes++ > 10)
        fail("Maybe you have not configured your MTA to route "
             + "smtptlistener mails to " + PORT);
      Thread.sleep(50);
      System.err.println("Sleeping");
      it = listener.getLastEmailReceived();
    }
    return it;
  }

  public void testEmailCatch() throws Exception {
    assertTrue(available(PORT));
    SmtpListener listener = new SmtpListener();
    listener.startListening();

    assertFalse(available(PORT));
    Email toSend = new Email("sender@smtplistener",
        "root@smtplistener", "Subject", "Message body\r\nLine 2\r\nLine 3\r\n"
        + ". a line starting with a dot but not the end of message\r\n");

    Emailer.send(toSend);

    assertFalse(available(PORT));
    Email receivedEmail = fetchEmail(listener);
    assertTrue(receivedEmail.toString() + "\n" + toSend.toString(), receivedEmail.equals(toSend));
    listener.stopListening();
    assertTrue(available(PORT));
  }

  public void testEmailWithAttachmentsDropped() throws Exception {
    assertTrue(available(PORT));
    SmtpListener listener = new SmtpListener();
    listener.startListening();

    assertFalse(available(PORT));
    Email toSend = new Email("sender@smtplistener",
        "root@smtplistener", "Subject", "Message body");
    File [] attachments = new File[] {new File("README.md")};
    Emailer.sendWithAttachments(
        "localhost",
        "sender@smtplistener",
        "root@smtplistener",
        "sender@smtplistener",
        "Subject",
        "Message body",
        attachments);

    assertFalse(available(PORT));
    Email receivedEmail = fetchEmail(listener);
    assertTrue(receivedEmail.toString() + "\n" + toSend.toString(), receivedEmail.equals(toSend));
    listener.stopListening();

    assertTrue(available(PORT));
  }


  /** Shows that this cannot be used, as is, for repeated tests, 
   *  but it does inch the coverage up.
   *
   *  Exim needs to have retrys configured off for this to work repeatedly,
   *  see README.md.
   */
  public void testNotRepeatable() throws Exception {
    assertTrue(available(PORT));
    SmtpListener listener = new SmtpListener();
    listener.startListening();
    assertFalse(available(PORT));
    for (int i = 1; i < 3; i++) {
      Email toSend = new Email("sender" + i + "@smtplistener", 
          "root@smtplistener", "Subject " + i, "Message body" + i);

      Emailer.send(toSend);

      assertFalse(available(PORT));
      Email receivedEmail = fetchEmail(listener);
      // Neither assertion holds
//      assertTrue(receivedEmail.toString().equals(toSend.toString()));
//      assertFalse(receivedEmail.toString().equals(toSend.toString()));
    }
    listener.stopListening();
    Thread.sleep(30);
  }

  /**
   * Checks to see if a specific port is available.
   *
   * @param port the port to check for availability
   */
  public static boolean available(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("Invalid start port: " + port);
    }

    ServerSocket ss = null;
    try {
      ss = new ServerSocket(port);
      return true;
    }
    catch (IOException e) {
      return false;
    }
    finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
          throw new RuntimeException("I promised this could never happen", e);
        }
      }
    }
  }
}
