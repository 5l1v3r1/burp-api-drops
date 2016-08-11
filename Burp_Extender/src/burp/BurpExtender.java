package burp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.PrintWriter;
import java.net.URLEncoder;


import burp.CAESOperator; //AES�ӽ����㷨��ʵ����
import burp.CUnicode; //unicode�����ʵ����
import burp.IParameter;


public class BurpExtender implements IBurpExtender, IHttpListener
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
    
    // implement IBurpExtender
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	//PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true); ����д���Ƕ��������ʵ����������ı��������µı���������֮ǰclass�е�ȫ�ֱ����ˡ�
    	//stdout.println("testxx");
    	//System.out.println("test"); ���������burp��
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName("AES encrypt Java edition"); //�������
        callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
    }

    @Override
    public void processHttpMessage(int toolFlag,boolean messageIsRequest,IHttpRequestResponse messageInfo)
    {
		List<String> paraWhiteList = new ArrayList<String>(); //�������������������еĲ���ֵ�����м��ܼ���
		Map<String, String> ParaMap = new HashMap<String, String>();//�����ƣ�д��һ��;���map������python�е�dict��
		paraWhiteList.add("android");
		
    	if (toolFlag == 64 || toolFlag == 16 || toolFlag == 32 || toolFlag == 4){ //��ͬ��toolflag�����˲�ͬ��burp��� https://portswigger.net/burp/extender/api/constant-values.html#burp.IBurpExtenderCallbacks
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
                List<IParameter> paraList = analyzeRequest.getParameters();//��body��json��ʽ��ʱ���������Ҳ����������ȡ����ֵ�ԣ�ţ��������PARAM_JSON�ȸ�ʽ����ͨ��updateParameter���������¡�
                //�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ�
                //��ȡ���ֲ�������Ϣ�岿�ֵļ��� 
                
                
                //�ж�һ�������Ƿ����ļ��ϴ�������
    			boolean isFileUploadRequest =false;
    			for (String header : headers){
    				//stdout.println(header);
    				if (header.toLowerCase().indexOf("content-type")!=-1 && header.toLowerCase().indexOf("boundary")!=-1){//ͨ��httpͷ�е������ж���������Ƿ����ļ��ϴ�������
    					isFileUploadRequest = true;
    				}
    			}
    			
    			//*******************encrypt each parameter with AES **********************//
    			if (isFileUploadRequest == false){ //���ļ��ϴ������󣬶����еĲ����������ܴ���
	    			byte[] new_Request = messageInfo.getRequest();
	    			for (IParameter para : paraList){// ѭ����ȡ�������ж����ͣ����м��ܴ�����ٹ����µĲ������ϲ����µ�������С�
	    				if ((para.getType() == 0 || para.getType() == 1) && !paraWhiteList.contains(para.getName())){ 
	    					//getTpe()�������жϲ��������Ǹ�λ�õģ�cookie�еĲ����ǲ���Ҫ���м��ܴ���ġ���Ҫ�ų��������еĲ�����
		    				//����Ҫע����ǣ����������͹�6�֣����body�еĲ�����json����xml��ʽ����Ҫ�����жϡ�
	    					String key = para.getName(); //��ȡ����������
		    				String value = para.getValue(); //��ȡ������ֵ
		    				//stdout.println(key+":"+value);
		    				CAESOperator aes = new CAESOperator(); //ʵ�������ܵ���
		    				String aesvalue;
		    				aesvalue = aes.encrypt(value); //��valueֵ���м���
		    				aesvalue = URLEncoder.encode(aesvalue); //��Ҫ����URL���룬��������= �������ַ����²����ж��쳣
		    				stdout.println(key+":"+value+":"+aesvalue); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
		    				//���°��ķ�������
		    				//���²���
		    				IParameter newPara = helpers.buildParameter(key, aesvalue, para.getType()); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
		    				//IParameter newPara = helpers.buildParameter(key, aesvalue, PARAM_BODY); //Ҫʹ�����PARAM_BODY �ǲ�����Ҫ��ʵ����IParameter�ࡣ
		    				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ������
		    				// new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
	    				}
	    			}
	    			messageInfo.setRequest(new_Request);//���������µ������
    			}
    			/* to verify the updated result
    			for (IParameter para : helpers.analyzeRequest(messageInfo).getParameters()){
    				stdout.println(para.getValue());
    			}
    			*/
    			//*******************encrypt each parameter with AES **********************//
    			
    			//*******************recalculate sign**************************//
    			if (isFileUploadRequest == false){ //��ĳЩ�����������������������
	    			byte[] new_Request = messageInfo.getRequest();
	    			for (IParameter para : paraList){// ѭ����ȡ�������ж����ͣ����м��ܴ�����ٹ����µĲ������ϲ����µ�������С�
	    				if (!paraWhiteList.contains(para.getName())){//���ڰ������еĲ����ͷŵ�ParaMap�У�Ҳ������Ҫ����sign����Ĳ����б�
	    					String key = para.getName(); //��ȡ����������
		    				String value = para.getValue(); //��ȡ������ֵ
		    				ParaMap.put(key, value);
		    				//stdout.println(key+":"+value);
		    				CRecalculater resign = new CRecalculater(); //ʵ�������ܵ���
		    				String newSign;
		    				newSign = resign.sign(ParaMap); //��valueֵ���м���
		    				stdout.println("New Sign:"+newSign); //�����extender��UI���ڣ�������ʹ������һЩ�ж�
		    				//���°��ķ�������
		    				//���²���
		    				IParameter newPara = helpers.buildParameter(key, aesvalue, para.getType()); //�����µĲ���,���������PARAM_JSON���ͣ���������ǲ����õ�
		    				//IParameter newPara = helpers.buildParameter(key, aesvalue, PARAM_BODY); //Ҫʹ�����PARAM_BODY �ǲ�����Ҫ��ʵ����IParameter�ࡣ
		    				new_Request = helpers.updateParameter(new_Request, newPara); //�����µ�������������Ƿ���һupdateParameter
		    				// new_Request = helpers.buildHttpMessage(headers, byte_body); //����޸���header�������޸���body��������ͨ��updateParameter��ʹ�����������
		    				
		    				//����������json���ݸ�ʽ�е�ʱ����Ҫ�õ����·�����
		    				//�����url�еĲ�����ֵ�� xxx=json��ʽ���ַ��� ������ʽ��ʱ��getParametersӦ�����޷���ȡ����ײ�ļ�ֵ�Եġ���Ҫ�������еĲ���Ҳ��Ҫʹ�����µķ�����
//			    			JSONObject jsonObject = JSON.parseObject(body);
//			    			JSONObject header = jsonObject.getJSONObject("header");
//			    			header.replace("sign", sign);
//			    			jsonObject.replace("header", header);
//			    			body = JSON.toJSONString(jsonObject);
		    				
	    				}
	    			}
	    			messageInfo.setRequest(new_Request);//���������µ������
    			}
    			/* to verify the updated result
    			for (IParameter para : helpers.analyzeRequest(messageInfo).getParameters()){
    				stdout.println(para.getValue());
    			}
    			*/
    			
    		}
    		
    		else{
    			//�����أ���Ӧ��
    			IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse()); //getResponse��õ����ֽ�����
    			List<String> header = analyzedResponse.getHeaders();
    			short statusCode = analyzedResponse.getStatusCode();
    			int bodyOffset = analyzedResponse.getBodyOffset();
    			if (statusCode==200){
    				try{
	    				CAESOperator aes = new CAESOperator();
	    				String resp = new String(messageInfo.getResponse());
	                    String body = resp.substring(bodyOffset);
	                    String deBody= aes.decrypt(body);
	                    deBody = deBody.replace("\"", "\\\"");
	                    String UnicodeBody = (new CUnicode()).unicodeDecode(deBody);
	                    //String newBody = body +"\r\n" +UnicodeBody; //���µĽ��ܺ��body�����ɵ�body����
	                    String newBody = UnicodeBody;
	                    byte[] bodybyte = newBody.getBytes();
	                    //���°��ķ�����buildHttpMessage
	                    messageInfo.setResponse(helpers.buildHttpMessage(header, bodybyte));
    				}catch(Exception e){
    					stdout.println(e);
    				}
    			}
    			
    		}	    		
    	}
    		
    }
}