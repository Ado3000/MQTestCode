import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
//import com.ibm.mq.pcf.CMQCFC;
//import com.ibm.mq.pcf.PCFMessage;
//import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;

/**
 * Simple MQ Service to read messages from a local queue and transform to DOM
 */
public class MQService {

	private static final String COMMAND_QUEUE_NAME = "STATQ.CMD";
	private static final String INBOUND_QUEUE_NAME = "Q1";
	static com.ibm.mq.headers.MQRFH2.Element[] propertyArray;
	static com.ibm.mq.headers.MQRFH2.Element[] folderElements;
	private MQGetMessageOptions cmdGmo = null;
	private MQMessage cmdMsg = null;
	private MQQueue cmdQueue = null;
	private MQQueue queue = null;
	private MQQueueManager qmgr = null;
	static MQMessage msg = new MQMessage();
	// private PCFMessage request = null;
	// private PCFMessage resp[] = null;
	// private PCFMessageAgent agent = null;

	/**
	 * Main entry point
	 * 
	 * @param args
	 *            - command line arguments (Qmanager and Queue)
	 */
	public static void main(String[] args) throws MQException {

		MQService mqService = null;
		try {
			(mqService = new MQService()).init(args[0]).run();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mqService != null)
				mqService.cleanup();
		}
	}
	

	// @SuppressWarnings("deprecation")
	public MQService init(String args) throws MQException {

		try {

			// Create a connection to the QueueManager
			qmgr = new MQQueueManager(args);

			// PCFMessageAgent agent = new PCFMessageAgent(qmgr);

			// Define admin queue
			// request = new PCFMessage (CMQCFC.MQCMD_CREATE_Q);
			// request.addParameter(CMQC.MQCA_Q_NAME, COMMAND_QUEUE_NAME);
			// request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
			// request.addParameter(CMQCFC.MQIACF_REPLACE, CMQCFC.MQRP_YES);
			//
			// if ((resp = agent.send(request)) != null && resp.length == 1) {
			// if (resp[0].getReason() == CMQC.MQRC_NONE) {
			// cmdQueue = qmgr.accessQueue(COMMAND_QUEUE_NAME,
			// CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF);
			// cmdGmo = new MQGetMessageOptions();
			// cmdGmo.options = CMQC.MQGMO_NO_WAIT |
			// CMQC.MQGMO_FAIL_IF_QUIESCING;// |
			// CMQC.MQGMO_ACCEPT_TRUNCATED_MSG;
			// cmdGmo.matchOptions = CMQC.MQMO_NONE;
			// cmdMsg = new MQMessage();
			// System.out.println(new Date()+" - Command queue created ok!");
			// } else {
			// System.out.println(new Date()+" - Failed to create command queue
			// with MQRC "+resp[0].getReason());
			// }
			// } else {
			// System.out.println(new Date()+" - Failed to create command
			// queue");
			// }

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (qmgr != null)
				qmgr.close();
		}

		return this;

	}

	public void run() throws MQException, MQDataException, IOException {
		cmdGmo = new MQGetMessageOptions();
		cmdGmo.options = CMQC.MQGMO_NO_WAIT | CMQC.MQGMO_FAIL_IF_QUIESCING;
		cmdGmo.matchOptions = CMQC.MQMO_NONE;
		cmdMsg = new MQMessage();
		try {
			MQException.logExclude(CMQC.MQRC_NO_MSG_AVAILABLE);
			while (!"shut down".equals(getCmd())) {
				// Not sure how should be done around this part...maybe a check
				// to see what CMD is in message

				boolean thereAreMessages = true;
				while (thereAreMessages) {

					try {

						// Control behaviour of MQGet
						MQGetMessageOptions gmo = new MQGetMessageOptions();

						// Open an MQ Queue associated with the QManager and
						// define MQI Constants
						// Open the queue to get messages using the
						// queue-defined default and call fail if in quiescing
						// state
						queue = qmgr.accessQueue(INBOUND_QUEUE_NAME,
								CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_RESOLVE_LOCAL_Q);

						// set Get Options
						gmo.options = CMQC.MQGMO_WAIT | CMQC.MQGMO_FAIL_IF_QUIESCING
								| CMQC.MQGMO_PROPERTIES_FORCE_MQRFH2 | CMQC.MQGMO_CONVERT;
						// Wait 60 seconds to see if any messages appear on the
						// queue
						gmo.waitInterval = 6000;

						try {
							// Do not show exception when queue is empty
							MQException.logExclude(CMQC.MQRC_NO_MSG_AVAILABLE);
							// Get the message off the queue
							queue.get(msg, gmo);

							// Build xml document
							javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory
									.newInstance();
							factory.setIgnoringElementContentWhitespace(true);
							factory.setNamespaceAware(true);
							javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
							Document document = builder.newDocument();

							Element mainRootElement = document.createElement("Context");
							document.appendChild(mainRootElement);

							// mainRootElement.appendChild(propertiesNode(document));
							mainRootElement.appendChild(getMQMD(msg, document));

							// if the message format is MQRFH2
							if (msg.format.equals(CMQC.MQFMT_RF_HEADER_2)) {

								MQRFH2 rfh = new MQRFH2();
								mainRootElement.appendChild(getMQRFH2(document, rfh));

							}
							StreamResult result = new StreamResult(new File("C:\\test.xml"));
							Transformer transformer = TransformerFactory.newInstance().newTransformer();
							System.out.println("XML:");
							transformer.transform(new DOMSource(document), result);
							System.out.println();
							System.out.println("\nXML DOM Created Successfully..");

							byte buf[] = new byte[msg.getDataLength()];
							msg.readFully(buf);
							System.out.printf("Read(%s): %s\n", asHex(msg.messageId), new String(buf));

							qmgr.commit();

						} catch (MQException e) {

							if (e.reasonCode != CMQC.MQRC_NO_MSG_AVAILABLE) {
								qmgr.backout();
								throw e;
							}

							thereAreMessages = false;

						} finally {
							MQException.logInclude(CMQC.MQRC_NO_MSG_AVAILABLE);
						}

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (queue != null)
							queue.close();

						if (qmgr != null)
							qmgr.close();
					}
				}

				if ("shut down".equals(getCmd()))
					break;

				// Do something else...
			}
			qmgr.commit();
		} catch (MQException e) {
			if (e.getReason() != CMQC.MQRC_NO_MSG_AVAILABLE) {
				try {
					qmgr.backout();
				} catch (Exception e2) {
					System.err.println(
							new Date() + " - An exception occurred backing out MQ transation: " + e.getMessage());
					e.printStackTrace();
				}
				throw e;
			}
		} catch (Exception e) {
			try {
				qmgr.backout();
			} catch (Exception e2) {
				System.err
						.println(new Date() + " - An exception occurred backing out MQ transation: " + e.getMessage());
				e.printStackTrace();
			}
			throw e;
		} finally {
			// Only report max five statistics messages a second
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// Do nothing!
			}

			MQException.logInclude(CMQC.MQRC_NO_MSG_AVAILABLE);
		}

	}

	private String getCmd() {
		try {
			if (cmdQueue != null) {
				cmdQueue.get(cmdMsg, cmdGmo);
				byte cmdBuf[] = new byte[cmdMsg.getDataLength()];
				cmdMsg.readFully(cmdBuf);
				String cmd = new String(cmdBuf);
				System.out.println(new Date() + " - Got cmd: " + cmd);

				return cmd;
			}
		} catch (MQException e) {
			if (e.reasonCode != CMQC.MQRC_NO_MSG_AVAILABLE) {
				System.err.println("Failed to get cmd: " + e.getMessage());
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.err.println("Failed to get cmd: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * private static Node propertiesNode(Document document) { Element
	 * destionationQ = document.createElement("DestinationQ"); Element
	 * properties = document.createElement("Properties");
	 * 
	 * properties.appendChild(destionationQ);
	 * properties.appendChild(propertyNode(document));
	 * 
	 * return properties; }
	 */

	/*
	 * private static Node propertyNode(Document document) { Element property =
	 * document.createElement("Property"); Element propertyName =
	 * document.createElement("Name"); Element propertyValue =
	 * document.createElement("Value");
	 * 
	 * property.appendChild(propertyName); property.appendChild(propertyValue);
	 * 
	 * return property;
	 * 
	 * }
	 */

	// get the Message Descript (MD) variables and create XML Part
	public static Node getMQMD(MQMessage srcMessage, Document document) {
		
		String version = String.valueOf(srcMessage.getVersion());
		String accountingToken = new String(asHex(srcMessage.accountingToken));
		String applicationIdData = srcMessage.applicationIdData;
		String applicationOriginData = srcMessage.applicationOriginData;
		String backoutCount = String.valueOf(srcMessage.backoutCount);
		String characterSet = String.valueOf(srcMessage.characterSet);
		String correlationId = new String(asHex(srcMessage.correlationId));
		String encoding = String.valueOf(srcMessage.encoding);
		String expiry = String.valueOf(srcMessage.expiry);
		String feedback = String.valueOf(srcMessage.feedback);
		String format = srcMessage.format;
		String groupId = new String(asHex(srcMessage.groupId));
		String messageFlags = String.valueOf(srcMessage.messageFlags);
		String messageId = new String(asHex(srcMessage.messageId));
		String messageSeqenceNumber = String.valueOf(srcMessage.messageSequenceNumber);
		String messageType = String.valueOf(srcMessage.messageType);
		String offset = String.valueOf(srcMessage.offset);
		String originalLength = String.valueOf(srcMessage.originalLength);
		String persistence = String.valueOf(srcMessage.persistence);
		String priority = String.valueOf(srcMessage.priority);
		String putApplicationName = srcMessage.putApplicationName;
		String putApplicationType = String.valueOf(srcMessage.putApplicationType);
		String replyToQueueManagerName = srcMessage.replyToQueueManagerName;
		String replyToQ = srcMessage.replyToQueueName;
		System.out.println( srcMessage.replyToQueueName);
		String report = String.valueOf(srcMessage.report);
		String usrID = srcMessage.userId;

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String putDteTime = formatter.format(srcMessage.putDateTime.getTime());

		Element mdElement = document.createElement("MQMD");

		// mdElement.setAttribute("Test", "123")
		mdElement.appendChild(getElements(document, "Version", version));
		mdElement.appendChild(getElements(document, "Report", report));
		mdElement.appendChild(getElements(document, "MsgType", messageType));
		mdElement.appendChild(getElements(document, "Expiry", expiry));
		mdElement.appendChild(getElements(document, "Feedback", feedback));
		mdElement.appendChild(getElements(document, "Encoding", encoding));
		mdElement.appendChild(getElements(document, "CodedCharSetID", characterSet));
		mdElement.appendChild(getElements(document, "Format", format));
		mdElement.appendChild(getElements(document, "Priority", priority));
		mdElement.appendChild(getElements(document, "Persistance", persistence));
		mdElement.appendChild(getElements(document, "MsgId", messageId));
		mdElement.appendChild(getElements(document, "CorrelId", correlationId));
		mdElement.appendChild(getElements(document, "BackoutCount", backoutCount));
		mdElement.appendChild(getElements(document, "ReplyToQ", replyToQ));
		mdElement.appendChild(getElements(document, "ReplyToQMgr", replyToQueueManagerName));
		mdElement.appendChild(getElements(document, "UserIdentifier", usrID));
		mdElement.appendChild(getElements(document, "AccountingToken", accountingToken));
		mdElement.appendChild(getElements(document, "ApplIdentityData", applicationIdData));
		mdElement.appendChild(getElements(document, "PutApplType", putApplicationType));
		mdElement.appendChild(getElements(document, "PutApplName", putApplicationName));
		mdElement.appendChild(getElements(document, "PutDateTime", putDteTime));
		mdElement.appendChild(getElements(document, "ApplOriginData", applicationOriginData));
		// Remaining fields empty if version is less than MQMD_VERSION_2
		mdElement.appendChild(getElements(document, "GroupId", groupId));
		mdElement.appendChild(getElements(document, "MsgSeqNumber", messageSeqenceNumber));
		mdElement.appendChild(getElements(document, "Offset", offset));
		mdElement.appendChild(getElements(document, "MsgFlags", messageFlags));
		mdElement.appendChild(getElements(document, "OriginalLength", originalLength));

		return mdElement;

	}
	
	// get the RFH2 header variables and create XML Part
	public static Node getMQRFH2(Document document, MQRFH2 rfhMessage) throws DOMException, IOException, MQDataException {
		rfhMessage = new MQRFH2();
		rfhMessage.read(msg);
		folderElements = rfhMessage.getFolders();
		Element rfhElement = document.createElement("MQRFH2");

		// rfhElement.setAttribute("Test", "345");
		rfhElement.appendChild(getElements(document, "StrucId", rfhMessage.getStrucId()));
		rfhElement.appendChild(getElements(document, "Version", String.valueOf(rfhMessage.getVersion())));
		rfhElement.appendChild(getElements(document, "StrucLength", String.valueOf(rfhMessage.getStrucLength())));
		rfhElement.appendChild(getElements(document, "Encoding", String.valueOf(rfhMessage.getEncoding())));
		rfhElement.appendChild(getElements(document, "CodedCharSetId", String.valueOf(rfhMessage.getCodedCharSetId())));
		rfhElement.appendChild(getElements(document, "Format", rfhMessage.getFormat()));
		rfhElement.appendChild(getElements(document, "Flags", String.valueOf(rfhMessage.getFlags())));
		rfhElement.appendChild(getElements(document, "NameValueCCSID", String.valueOf(rfhMessage.getNameValueCCSID())));

		Element nameValueDataElement = document.createElement("NameValueData");
		rfhElement.appendChild(nameValueDataElement);
		
		//Read from folders in NameValueData and turn into XML
		for (int i = 0; i <= folderElements.length-1; i++) {
			Element folderDocElement = document.createElement(folderElements[i].getName());
			folderDocElement.appendChild(getElements(document, folderElements[i].getChildren()[0].getName(),
					String.valueOf(folderElements[i].getChildren()[0].getContent())));
			nameValueDataElement.appendChild(folderDocElement);
		}

		return rfhElement;
	}

	// utility method to create text node
	private static Node getElements(Document doc, String name, String value) {
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value));
		return node;
	}

	private final static char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f' };

	/**
	 * This method BinHex encodes the provided byte array
	 * 
	 * @param buf
	 *            - the byte array to BinHex encode
	 * @return - the BinHex encoded result
	 */
	public static String asHex(byte[] bytes) {
		char buf[] = new char[bytes.length * 2];

		for (int i = 0; i < bytes.length; i++) {
			buf[2 * i] = HEX_DIGITS[(bytes[i] >> 4) & 15];
			buf[2 * i + 1] = HEX_DIGITS[bytes[i] & 15];
		}

		return new String(buf);
	}

	// @SuppressWarnings("deprecation")
	private void cleanup() {

		System.err.println("Method Cleanup");

		// try {
		//
		// request.addParameter(CMQC.MQCA_Q_NAME, COMMAND_QUEUE_NAME);
		// request.addParameter(CMQCFC.MQIACF_PURGE, CMQCFC.MQPO_YES);
		// resp = agent.send(request);
		//
		// if (resp != null && resp.length == 1) {
		// if (resp[0].getReason() == CMQC.MQRC_NONE)
		// System.out.println(new Date()+" - Command queue deleted ok!");
		// else
		// System.err.println("Unable to delete command queue,
		// MQRC="+resp[0].getReason());
		// }
		//
		// } catch (Exception e) {
		// System.err.println("Error: "+e.getMessage());
		// e.printStackTrace();
		// }

	}

}
