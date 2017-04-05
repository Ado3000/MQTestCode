

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;

public class XMLParserTest {
	GregorianCalendar calendar = new GregorianCalendar();
	

	public static void main(String[] args) throws MQException, IOException, MQDataException {
		MQQueueManager queuemgr = new MQQueueManager("QM_TEST");
		MQQueue queue = queuemgr.accessQueue("Q1", CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_OUTPUT);
		MQMessage message = new MQMessage();
		MQRFH2 header = new MQRFH2();
		
		mqmdBuilder(message);
		queue.put(message);
		

//		mqrfh2Builder(header);
//		message.format = CMQC.MQFMT_RF_HEADER_2;
//		header.write(message);
//		queue.put(message);
		
	}
	
	
/*	class Outtest extends OutputStream {
		
		private MQMessage message;

		public Outtest(MQMessage message){
			this.message = message;
		}

		@Override
		public void write(int b) throws IOException {
			// TODO Auto-generated method stub
			message.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			// TODO Auto-generated method stub
			message.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			message.write(b, off, len);
		}
		
		
		
	}*/
	
	
	/*public void test() throws IOException{
		MQMessage sourceMessage = new MQMessage();
		MQMessage targetMessage = new MQMessage();
		Outtest test = new Outtest(targetMessage);
		byte [] buf = new byte [sourceMessage.getDataLength()];
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		sourceMessage.readFully(buf);
		
		test.write(bais.read());
		
	}*/

	//Read the MQMD part of the XML file and puts it into the MQMD part of the MQ message
	public static MQMessage mqmdBuilder(MQMessage msg) {
		MQGetMessageOptions gmo = new MQGetMessageOptions();
		gmo.matchOptions = CMQC.MQMO_NONE;
		
		
		try {

			File inputFile = new File("test.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("MQMD");
			System.out.println("----------------------------");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String version = eElement.getElementsByTagName("Version").item(0).getTextContent();
					String report = eElement.getElementsByTagName("Report").item(0).getTextContent();
					String msgType = eElement.getElementsByTagName("MsgType").item(0).getTextContent();
					String expiry = eElement.getElementsByTagName("Expiry").item(0).getTextContent();
					String feedBack = eElement.getElementsByTagName("Feedback").item(0).getTextContent();
					String encode = eElement.getElementsByTagName("Encoding").item(0).getTextContent();
					String codedCharSetID = eElement.getElementsByTagName("CodedCharSetID").item(0).getTextContent();
					String format = eElement.getElementsByTagName("Format").item(0).getTextContent();
					String priority = eElement.getElementsByTagName("Priority").item(0).getTextContent();
					String persistance = eElement.getElementsByTagName("Persistance").item(0).getTextContent();
					String msgID = eElement.getElementsByTagName("MsgId").item(0).getTextContent();
					String correlID = eElement.getElementsByTagName("CorrelId").item(0).getTextContent();
					String backoutCount = eElement.getElementsByTagName("BackoutCount").item(0).getTextContent();
					String replyToQ = eElement.getElementsByTagName("ReplyToQ").item(0).getTextContent();
					String replyToQmgr = eElement.getElementsByTagName("ReplyToQMgr").item(0).getTextContent();
					String userIdentifier = eElement.getElementsByTagName("UserIdentifier").item(0).getTextContent();
					String accountingToken= eElement.getElementsByTagName("AccountingToken").item(0).getTextContent();
					String applIdentityData = eElement.getElementsByTagName("ApplIdentityData").item(0).getTextContent();
					String putApplType = eElement.getElementsByTagName("PutApplType").item(0).getTextContent();
					String putApplName = eElement.getElementsByTagName("PutApplName").item(0).getTextContent();
					String applOriginData = eElement.getElementsByTagName("ApplOriginData").item(0).getTextContent();
					String groupId = eElement.getElementsByTagName("GroupId").item(0).getTextContent();
					String msgSeqNumber = eElement.getElementsByTagName("MsgSeqNumber").item(0).getTextContent();
					String offSet = eElement.getElementsByTagName("Offset").item(0).getTextContent();
					String msgFlags = eElement.getElementsByTagName("MsgFlags").item(0).getTextContent();
					String orgLength = eElement.getElementsByTagName("OriginalLength").item(0).getTextContent();

					msg.setVersion(Integer.parseInt(version));

					msg.report = Integer.parseInt(report);
					msg.messageType = Integer.parseInt(msgType);
					msg.expiry = Integer.parseInt(expiry);
					msg.feedback = Integer.parseInt(feedBack);
					msg.encoding = Integer.parseInt(encode);
					msg.characterSet = Integer.parseInt(codedCharSetID);
					msg.format = format;
					msg.priority = Integer.parseInt(priority);
					msg.persistence = Integer.parseInt(persistance);
					msg.backoutCount = Integer.parseInt(backoutCount);
					msg.replyToQueueName = replyToQ;
					msg.replyToQueueManagerName = replyToQmgr;
					msg.userId = userIdentifier;
					msg.applicationIdData = applIdentityData;
					msg.putApplicationType = Integer.parseInt(putApplType);
					msg.putApplicationName = putApplName;
					msg.applicationOriginData = applOriginData;
					msg.messageSequenceNumber = Integer.parseInt(msgSeqNumber);
					msg.offset = Integer.parseInt(offSet);
					msg.messageFlags = Integer.parseInt(msgFlags);
					msg.originalLength = Integer.parseInt(orgLength);
					msg.messageId = msgID.getBytes();
					msg.correlationId = correlID.getBytes();
					msg.accountingToken = accountingToken.getBytes();
					msg.groupId = groupId.getBytes();
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return msg;
	}


	//Parses XML to MQRFH2 header
	public static MQRFH2 mqrfh2Builder(MQRFH2 rfhMsg) {
		try {

			File inputFile = new File("test.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("MQRFH2");
			System.out.println("----------------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
					Element eElement = (Element) nNode;
	
					String version = eElement.getElementsByTagName("Version").item(0).getTextContent();
					String structLength = eElement.getElementsByTagName("StrucLength").item(0).getTextContent();
					String encoding = eElement.getElementsByTagName("Encoding").item(0).getTextContent();
					String codedCharSet = eElement.getElementsByTagName("CodedCharSetId").item(0).getTextContent();
					String format = eElement.getElementsByTagName("Format").item(0).getTextContent();
					String flags = eElement.getElementsByTagName("Flags").item(0).getTextContent();
					String nameValueCCSID = eElement.getElementsByTagName("NameValueCCSID").item(0).getTextContent();

					rfhMsg.setFlags(Integer.parseInt(flags));
					rfhMsg.setFormat(format);
					rfhMsg.setNameValueCCSID(Integer.parseInt(nameValueCCSID));
					rfhMsg.setEncoding(Integer.parseInt(encoding));
					rfhMsg.setCodedCharSetId(Integer.parseInt(codedCharSet));
					//TODO: Loop over all individual value properties
//					rfhMsg.setFieldValue("usr", "Test", usr);
//					rfhMsg.set
//					rfhMsg.setFieldValue("mcd", "StrucLength", structLength.getBytes());
					
					NodeList nameValueList = eElement.getElementsByTagName("NameValueData");
					System.out.println("----------------------------");
					
					for (int i = 0; i <= nList.getLength(); i++) {
						Node nameValueNode = nameValueList.item(temp);
							Element nameValueElement = (Element) nameValueNode;
							
							rfhMsg.setFieldValue("jms", nameValueElement.getChildNodes().item(1).getChildNodes().item(1).getNodeName(), nameValueElement.getChildNodes().item(1).getChildNodes().item(1).getChildNodes().item(0).getNodeValue());
							rfhMsg.setFieldValue("mcd", nameValueElement.getChildNodes().item(3).getChildNodes().item(1).getNodeName(), nameValueElement.getChildNodes().item(3).getChildNodes().item(1).getChildNodes().item(0).getNodeValue());
							rfhMsg.setFieldValue("usr", nameValueElement.getChildNodes().item(5).getChildNodes().item(1).getNodeName(), nameValueElement.getChildNodes().item(5).getChildNodes().item(1).getChildNodes().item(0).getNodeValue());		
					}
		
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rfhMsg;
	}
	
}
