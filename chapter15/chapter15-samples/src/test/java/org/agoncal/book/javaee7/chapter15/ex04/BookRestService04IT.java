package org.agoncal.book.javaee7.chapter15.ex04;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.net.URI;
import javax.ws.rs.core.Link;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.junit.AfterClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;

/**
 * @author Antonio Goncalves
 *         APress Book - Beginning Java EE 7 with Glassfish 4
 *         http://www.apress.com/
 *         http://www.antoniogoncalves.org
 *         --
 */
public class BookRestService04IT {

  // ======================================
  // =             Attributes             =
  // ======================================

  private static HttpServer server;
  private static URI uri = UriBuilder.fromUri("http://localhost/chapter15-samples-1.0/rs").port(8080).build();
  private static Client client = ClientBuilder.newClient();

  private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><book04><description>Science fiction comedy book</description><illustrations>false</illustrations><isbn>1-84023-742-2</isbn><nbOfPage>354</nbOfPage><price>12.5</price><title>The Hitchhiker's Guide to the Galaxy</title></book04>";
    private static GlassFishRuntime runtime;
    private static GlassFish gf;

 @BeforeClass
  public static void init() throws IOException, GlassFishException {
    // we're really getting into realm where Arquillian would be much more comfortable.
    // for details on using embedded glassfish API see Embedded Glassfish Guide
    // https://docs.oracle.com/cd/E18930_01/html/821-2424/gjldt.html#
     runtime = GlassFishRuntime.bootstrap();
     GlassFishProperties prop = new GlassFishProperties();
     prop.setPort("http-listener", 8080);
     gf = runtime.newGlassFish(prop);
     gf.start();
     
     String result = gf.getDeployer().deploy(new File("target/chapter15-samples-1.0.war"));
     if (result == null) {
         throw new IllegalStateException("Deployment failed");
     }
  }

  @AfterClass
  public static void stop() throws GlassFishException {
    gf.stop();
    gf.dispose();
    runtime.shutdown();
  }

  // ======================================
  // =              Unit tests            =
  // ======================================


  @Test
  public void shouldMarshallABook() throws JAXBException {
    // given
    Book04 book = new Book04("The Hitchhiker's Guide to the Galaxy", 12.5F, "Science fiction comedy book", "1-84023-742-2", 354, false);
    StringWriter writer = new StringWriter();
    JAXBContext context = JAXBContext.newInstance(Book04.class);
    Marshaller m = context.createMarshaller();
    m.marshal(book, writer);

    // then
    assertEquals(XML, writer.toString());
  }

  @Test
  public void shouldMarshallAListOfBooks() throws JAXBException {
    Books04 books = new Books04();
    books.add(new Book04("The Hitchhiker's Guide to the Galaxy", 12.5F, "Science fiction comedy book", "1-84023-742-2", 354, false));
    books.add(new Book04("The Hitchhiker's Guide to the Galaxy", 12.5F, "Science fiction comedy book", "1-84023-742-2", 354, false));
    StringWriter writer = new StringWriter();
    JAXBContext context = JAXBContext.newInstance(Books04.class);
    Marshaller m = context.createMarshaller();
    m.marshal(books, writer);
  }


  @Test
  public void shouldCreateABook() throws JAXBException {
    // given
    Book04 book = new Book04("The Hitchhiker's Guide to the Galaxy", 12.5F, "Science fiction comedy book", "1-84023-742-2", 354, false);
    StringWriter writer = new StringWriter();
    JAXBContext context = JAXBContext.newInstance(Book04.class);
    Marshaller m = context.createMarshaller();
    m.marshal(book, writer);

    // when
    Response response = client.target(uri).path("/04/books")
            .request().post(Entity.entity(book, "application/xml"));

    // then
    assertEquals(201, response.getStatus());
    assertTrue(response.getLocation().toString().startsWith("http://localhost:8080/chapter15-samples-1.0/rs/04/books"));

    // when
    response = client.target(response.getLocation())
            .request(MediaType.APPLICATION_XML).get();

    // then
    assertEquals(200, response.getStatus());
    assertTrue(response.hasEntity());

    System.out.println("########## " + response.getEntity());

    book = response.readEntity(Book04.class);
    assertEquals("The Hitchhiker's Guide to the Galaxy", book.getTitle());
    assertEquals("Science fiction comedy book", book.getDescription());
  }
  
  @Test
  public void shouldCreateABookViaPlaintext() {
      Response response = client.target(uri).path("/04/books")
              .request()
              .post(Entity.entity("Java EE Patterns","text/plain"));
      
      assertEquals(201, response.getStatus());
      
      URI bookUri = response.getLocation();
      System.out.println(bookUri);
      Book04 book = client.target(bookUri)
              .request(MediaType.APPLICATION_XML)
              .get(Book04.class);
      assertEquals("Java EE Patterns", book.getTitle());

      response = client.target(bookUri).request()
              .delete();
      
      assertEquals(204, response.getStatus());
  }
  
  @Test
  public void shouldDeleteABookViaLink() {
      Response response = client.target(uri).path("/04/books")
              .request()
              .post(Entity.entity("Java EE Patterns","text/plain"));
      
      assertEquals(201, response.getStatus());
      
      URI bookUri = response.getLocation();
      System.out.println(bookUri);
      response = client.target(bookUri)
              .request(MediaType.APPLICATION_XML)
              .get();
      Link deleteLink = response.getLink("DELETE");
      Book04 book = response.readEntity(Book04.class);
      assertEquals("Java EE Patterns", book.getTitle());
      response = client.target(deleteLink).request()
              .delete();
      
      assertEquals(204, response.getStatus());
  }  
  
  @Test
  public void test_Should_Return_Not_Found_When_Deleting_Non_Existing() {
      Response response = client.target(uri).path("/04/books")
              .path("999")
              .request()
              .delete();
      
      assertEquals(404, response.getStatus());
      String body = response.readEntity(String.class);
      assertEquals("Book 999 does not exist at 04/books/999", body);
  }
}