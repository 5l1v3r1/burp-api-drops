package burp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.net.URLEncoder;


import custom.AES_128; //AES�ӽ����㷨��ʵ����
import custom.JsonParser;
import custom.MD5;
import custom.MapSort;
import custom.Unicode; //unicode�����ʵ����

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meizu.bigdata.carina.common.util.TradeEncryptUtil;


public class BurpExtender implements IBurpExtender, IHttpListener
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
    private String ExtenderName = "AES encrypter for carina";
    private List<String> paraWhiteList = new ArrayList<String>();
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	stdout.println(ExtenderName);
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(ExtenderName); //�������
        callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
    }

    @Override
    public void processHttpMessage(int toolFlag,boolean messageIsRequest,IHttpRequestResponse messageInfo)
    {
    	if (toolFlag == (toolFlag&checkEnabledFor())){
    		//��ͬ��toolflag�����˲�ͬ��burp��� https://portswigger.net/burp/extender/api/constant-values.html#burp.IBurpExtenderCallbacks
        	Encrypter(messageIsRequest, messageInfo);
        	//ReSign(messageIsRequest, messageInfo);
    	}
    }
    
    public void Encrypter(boolean messageIsRequest,IHttpRequestResponse messageInfo){
		if (messageIsRequest){ //����������д���
			
			int flag = 3;//���ƴ���ģ�飬��������Ϣ�壬httpͷ
			IRequestInfo analyzeRequest;
			
			switch (flag) {
			case 1://������� �ǲ��������
			{
				analyzeRequest = helpers.analyzeRequest(messageInfo); //����Ϣ����н���
    			byte[] new_Request = messageInfo.getRequest();
    			//the method of get parameter
    			List<IParameter> paraList = analyzeRequest.getParameters();//��body��json��ʽ��ʱ���������Ҳ����������ȡ����ֵ�ԣ�ţ��������PARAM_JSON�ȸ�ʽ����ͨ��updateParameter���������¡�
                //�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ�
    			for (IParameter para : paraList){// ѭ����ȡ�������ж����ͣ����м��ܴ�����ٹ����µĲ������ϲ����µ�������С�
    				if ((para.getType() == 0 || para.getType() == 1) && !paraWhiteList.contains(para.getName())){ //getTpe()�������жϲ��������Ǹ�λ�õģ�cookie�еĲ����ǲ���Ҫ���м��ܴ���ġ���Ҫ�ų��������еĲ�����
	    				//��������7�ָ�ʽ��0��url������1��body������2��cookie���� ��helpers.updateParameterֻ֧�������֣���6��json����--���ֲ����ĸ����ø���body�ķ�����
    					String key = para.getName(); //��ȡ����������
	    				String value = para.getValue(); //��ȡ������ֵ
	    				//stdout.println(key+":"+value);
	    				String encryptedValue;
						try {
							encryptedValue = TradeEncryptUtil.encrypt(value);
							encryptedValue = URLEncoder.encode(encryptedValue); //��Ҫ����URL���룬��������= �������ַ����²����ж��쳣;
		    				stdout.println(key+":"+value+":"+encryptedValue); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
		    				IParameter newPara = helpers.buildParameter(key, encryptedValue, para.getType()); //�����µĲ���
		    				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ������
		    				messageInfo.setRequest(new_Request);//���������µ������
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    				}
    			}
				break;
			}
			case 2://���������header�����
			{
    			//the method of get header
				analyzeRequest = helpers.analyzeRequest(messageInfo); //ǰ��Ĳ��������Ѿ��޸���������������������ĸ���ǰ������Ҫ���»�ȡ��
				List<String> headers = analyzeRequest.getHeaders();
    			break;
			}
    		case 3:
    		{
    			//the method of get body
    			analyzeRequest = helpers.analyzeRequest(messageInfo); //ǰ��Ĳ��������Ѿ��޸���������������������ĸ���ǰ������Ҫ���»�ȡ��
    			List<String> headers = analyzeRequest.getHeaders();//ǩ��header�����Ѿ��ı䣬��Ҫ���»�ȡ
    			int bodyOffset = analyzeRequest.getBodyOffset();
    			byte[] byte_Request = messageInfo.getRequest();
    			String request = new String(byte_Request); //byte[] to String
                String body = request.substring(bodyOffset);
                byte[] byte_body = body.getBytes();  //String to byte[]
                
				try {
					String newBody = TradeEncryptUtil.encrypt(body);
					stdout.println(newBody);
					byte[] bodyByte = newBody.getBytes();
	    			byte[] new_Request = helpers.buildHttpMessage(headers, bodyByte); //����޸���header�������޸���body������ͨ��updateParameter��ʹ�����������
	    			messageInfo.setRequest(new_Request);//���������µ������
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
			default:
				/* to verify the updated result
				for (IParameter para : helpers.analyzeRequest(messageInfo).getParameters()){
					stdout.println(para.getValue());
				}
				*/
				break;
			}			
		}
		else{//�����أ���Ӧ��
			IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse()); //getResponse��õ����ֽ�����
			short statusCode = analyzedResponse.getStatusCode();
			List<String> header = analyzedResponse.getHeaders();
			String resp = new String(messageInfo.getResponse());
			int bodyOffset = analyzedResponse.getBodyOffset();//��Ӧ����û�в����ĸ���ģ������Ҫ�޸ĵ����ݶ���body��
            String body = resp.substring(bodyOffset);
			
			int flag = 2;
			
			switch (flag) {
			case 1://����header����������޸���response,ע��case2��Ӧ�ô��»�ȡheader���ݡ�
				break;
			case 2://����body
    			if (statusCode==200){
    				try{
	                    String deBody= TradeEncryptUtil.decrypt(body);
	                    deBody = deBody.replace("\"", "\\\"");
	                    String UnicodeBody = (new Unicode()).unicodeDecode(deBody);
	                    String newBody = body +"\r\n" +UnicodeBody; //���µĽ��ܺ��body�����ɵ�body����
	                    //String newBody = UnicodeBody;
	                    byte[] bodybyte = newBody.getBytes();
	                    messageInfo.setResponse(helpers.buildHttpMessage(header, bodybyte));
    				}catch(Exception e){
    					stdout.println(e);
    				}
    			}
				break;
			default:
				break;
			}
		}	    		
    }
    
    public void ReSign(boolean messageIsRequest,IHttpRequestResponse messageInfo){
    	if (messageIsRequest){ //����������д���
			
			//��ȡ���ֲ�������Ϣ��ķ����������£��޷����֣�body��header��paramater
			IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo); //����Ϣ����н��� 
			//the method of get header
			List<String> headers = analyzeRequest.getHeaders(); //��ȡhttp����ͷ����Ϣ�����ؿ��Կ�����һ��python�е��б�java���ǽз���ʲô�ģ���ûŪ���
			//the method of get body
			int bodyOffset = analyzeRequest.getBodyOffset();
			byte[] byte_Request = messageInfo.getRequest();
			String request = new String(byte_Request); //byte[] to String
            String body = request.substring(bodyOffset);
            byte[] byte_body = body.getBytes();  //String to byte[]
			//the method of get parameter
            List<IParameter> paras = analyzeRequest.getParameters();
            Map<String, String> paraMap = new HashMap<>();
            
            String signPara = "sign";
            byte signParaType = 0;
            
            int flag = 1;
            
            switch (flag) {
			case 1://���ֱ�ӿ��Ի�ȡ��url������body������������json�Ͳ���
			{
				byte[] new_Request = messageInfo.getRequest();
				for (IParameter para:paras){
					String key = para.getName();
					String value = para.getValue();
					if (!paraWhiteList.contains(key)&&(para.getType()==1 ||para.getType()==0)){//ע�������para_encrypterģ����ʹ�õ���ͬһ��������
						paraMap.put(key, value);
					}
					if (key.equals(signPara)){
						signParaType = para.getType();
					}
				}
				//paraMap.put("key","secretkey");//1.��secret key�ļ�ֵ�����һ���Դ� 
				paraMap = MapSort.sortMapByKey(paraMap, "ASCENDING");
				String paraString = MapSort.combineMapEntry(paraMap, "&");
				paraString += "&key=secretkey";//2.��secret keyֱ�Ӹ��ӵ��ϲ����ַ���ĩβ
				String newSign = MD5.GetMD5Code(paraString);
				
				IParameter newPara = helpers.buildParameter("sign", newSign, signParaType); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ�������������Ƿ���һupdateParameter
				messageInfo.setRequest(new_Request);
				break;
			}
			case 2://���json�Ͳ�����
			{
				byte[] new_Request = messageInfo.getRequest();
				//�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ���Ҫ�������еĲ���Ҳ��Ҫʹ�����µķ�����
				paraMap = JsonParser.parseJson(body);
	            
				
				paraMap = MapSort.sortMapByKey(paraMap, "ASCENDING");
				String paraString = MapSort.combineMapEntry(paraMap, "&");
				paraString += "&key=secretkey";//2.��secret keyֱ�Ӹ��ӵ��ϲ����ַ���ĩβ
				String newSign = MD5.GetMD5Code(paraString);
	            
    			JSONObject jsonObject = JSON.parseObject(body);
    			//JSONObject header = jsonObject.getJSONObject("header");���ṹ
    			jsonObject.replace("sign", newSign);
    			body = JSON.toJSONString(jsonObject);
    			byte_body = body.getBytes();
				new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
				messageInfo.setRequest(new_Request);
			}
			default:
				break;
			}
    	}
    }
    
	public int checkEnabledFor(){
		//get values that should enable this extender for which Component.
		//��ͬ��toolflag�����˲�ͬ��burp��� https://portswigger.net/burp/extender/api/constant-values.html#burp.IBurpExtenderCallbacks
		int status = 0;
		int	TOOL_COMPARER = 512;
		int	TOOL_DECODER = 256;
		int	TOOL_EXTENDER = 1024;
		int	TOOL_INTRUDER = 32;
		int	TOOL_PROXY = 4;
		int	TOOL_REPEATER = 64;
		int	TOOL_SCANNER = 16;
		int	TOOL_SEQUENCER = 128;
		int	TOOL_SPIDER = 8;
		int	TOOL_SUITE = 1;
		int	TOOL_TARGET= 2;
		
		return status+TOOL_PROXY+TOOL_REPEATER+TOOL_SCANNER+TOOL_INTRUDER;
	}
}