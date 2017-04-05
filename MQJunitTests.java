import static org.junit.Assert.*;

import org.junit.Test;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.jms.JMSMessage;
import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;
import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueueConnection;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueueReceiver;
import com.ibm.mq.jms.MQQueueSender;
import com.ibm.mq.jms.MQQueueSession;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.FixMethodOrder;
import org.junit.Ignore;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class MQJunitTests {
	
   @Parameterized.Parameters
   public static Collection<Object[]> primeNumbers() {
      return Arrays.asList(new Object[][] {
         { "Test1", "test2", "Test3"}
      });
   }
   
   static String qmName = "QM_APPLE";
   private String param1;
   private String param2;
   private String param3;
   static MQQueueManager qmanager;
   static MQQueue queue;
   byte buf[];
   MQMessage msg;
   Message message;
	
	@BeforeClass
	public static void init() throws MQException {
		 qmanager = new MQQueueManager(qmName);
		 queue = qmanager.accessQueue("Q1", CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_RESOLVE_LOCAL_Q);
		
	}

	public MQJunitTests(String param1, String param2, String param3) {
		this.param1 = param1;
		this.param2 = param2;
		this.param3 = param3;
	}
	
	/*
	 * Test for putting a JMS message on the queue 
	 * with the appropriate properties
	 */
	@Test
	@Ignore
	public void test01_PutSampleMessageOnQueue() throws IOException, JMSException, MQException {
		 MQQueueConnectionFactory cf = new MQQueueConnectionFactory();

	      // Config
	      cf.setHostName("Ado-Dator");
	      cf.setPort(1414);
	      cf.setQueueManager("QM_APPLE");
	      cf.setChannel("SYSTEM.DEF.SVRCONN");

	      MQQueueConnection connection = (MQQueueConnection) cf.createQueueConnection();
	      MQQueueSession session = (MQQueueSession) connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	      Queue queue = session.createQueue("queue:///Q1");
	      MQQueueSender sender =  (MQQueueSender) session.createSender((Queue) queue);
	      MQQueueReceiver receiver = (MQQueueReceiver) session.createReceiver((Queue) queue);      

	      long uniqueNumber = System.currentTimeMillis() % 1000;
	      message = (JMSTextMessage) session.createTextMessage("SimplePTP "+ uniqueNumber);
	      message.setStringProperty("usr", "Adnan");

	      // Start the connection
	      connection.start();

	      sender.send(message);

	}
	
	
	/*
	 * Test for getting a message from a queue and 
	 * transforming it into a XML-DOM document
	 */
	@Test
	public void test02_GetMessageFromQueue() throws JMSException, MQException, IOException, MQDataException, InterruptedException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		msg = new MQMessage();
		MQGetMessageOptions gmo = new MQGetMessageOptions();
		gmo.options = CMQC.MQGMO_WAIT | CMQC.MQGMO_FAIL_IF_QUIESCING
				| CMQC.MQGMO_PROPERTIES_FORCE_MQRFH2 | CMQC.MQGMO_CONVERT;
		queue.get(msg,gmo);

		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory
				.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setNamespaceAware(true);
		javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();

		Element mainRootElement = document.createElement("Context");
		document.appendChild(mainRootElement);

		// mainRootElement.appendChild(propertiesNode(document));
		mainRootElement.appendChild(MQService.getMQMD(msg, document));

		// if the message format is MQRFH2
		if (msg.format.equals(CMQC.MQFMT_RF_HEADER_2)) {

			MQRFH2 rfh = new MQRFH2();
			Node appendChild = mainRootElement.appendChild(MQService.getMQRFH2(document, rfh));

		}
		StreamResult result = new StreamResult(new File("C:\\test.xml"));
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		System.out.println("XML:");
		transformer.transform(new DOMSource(document), result);
		System.out.println();
		System.out.println("\nXML DOM Created Successfully..");
		
		byte buf[] = new byte[msg.getDataLength()];
		msg.readFully(buf, 0, msg.getDataLength());
		System.out.println(new String(buf));

		
	}
	
	
	/*
	 * Test that reads from a XML-DOM document and 
	 * transforms it into a MQMessage
	 */
	@Test
	@Ignore
	public void test03_PutMessageOnQueue() throws IOException, MQException, MQDataException {
		MQMessage msg = new MQMessage();
		if(msg.format == CMQC.MQFMT_RF_HEADER_2){
			XMLParserTest.mqrfh2Builder(new MQRFH2(msg));
		}
		else{
			XMLParserTest.mqmdBuilder(msg);
		}
		queue.put(msg);
	}
	
	
	/*
	 * Reads from the the JMS message that was put in
	 * test01 and verifies the correct properties
	 */
	@Test
	public void test04_GetSampleJmsMessage() {
		System.out.println("Fourth, param2=");
	}
	
	@AfterClass
	public static void cleanUp(){
		System.out.println("cleanUp");
	}

}
