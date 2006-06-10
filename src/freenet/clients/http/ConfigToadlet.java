package freenet.clients.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import freenet.client.HighLevelSimpleClient;
import freenet.config.Config;
import freenet.config.Option;
import freenet.config.SubConfig;
import freenet.node.Node;
import freenet.support.Bucket;
import freenet.support.BucketTools;
import freenet.support.HTMLEncoder;
import freenet.support.Logger;
import freenet.support.MultiValueTable;


// FIXME: add logging, comments
public class ConfigToadlet extends Toadlet {
	private Config config;
	private final Node node;
	
	ConfigToadlet(HighLevelSimpleClient client, Config conf, Node node) {
		super(client);
		config=conf;
		this.node = node;
	}

	public void handlePost(URI uri, Bucket data, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		StringBuffer errbuf = new StringBuffer();
		SubConfig[] sc = config.getConfigs();
		
		// FIXME this is stupid, use a direct constructor
		
		if(data.size() > 1024*1024) {
			this.writeReply(ctx, 400, "text/plain", "Too big", "Too much data, config servlet limited to 1MB");
			return;
		}
		byte[] d = BucketTools.toByteArray(data);
		String s = new String(d, "us-ascii");
		HTTPRequest request;
		try {
			request = new HTTPRequest("/", s);
		} catch (URISyntaxException e) {
			Logger.error(this, "Impossible: "+e, e);
			return;
		}
		
		String pass = request.getParam("formPassword");
		if(pass == null || !pass.equals(node.formPassword)) {
			MultiValueTable headers = new MultiValueTable();
			headers.put("Location", "/queue/");
			ctx.sendReplyHeaders(302, "Found", headers, null, 0);
			return;
		}
		
		for(int i=0; i<sc.length ; i++){
			Option[] o = sc[i].getOptions();
			String prefix = new String(sc[i].getPrefix());
			String configName;
			
			for(int j=0; j<o.length; j++){
				configName=o[j].getName();
				Logger.minor(this, "Setting "+prefix+"."+configName);
				
				// we ignore unreconized parameters 
				if(request.isParameterSet(prefix+"."+configName)) {
					if(!(o[j].getValueString().equals(request.getParam(prefix+"."+configName)))){
						Logger.minor(this, "Setting "+prefix+"."+configName+" to "+request.getParam(prefix+"."+configName));
						try{
							o[j].setValue(request.getParam(prefix+"."+configName));
						}catch(Exception e){
							errbuf.append(o[j].getName()+" "+e+"\n");
							Logger.error(this, "Caught "+e, e);
						}
					}
				}
			}
		}
		config.store();
		
		StringBuffer outbuf = new StringBuffer();
		
		ctx.getPageMaker().makeHead(outbuf, "Configuration Applied");
		
		if (errbuf.length() == 0) {
			outbuf.append("<div class=\"infobox infobox-success\">\n");
			outbuf.append("<div class=\"infobox-header\">\n");
			outbuf.append("Configuration Applied\n");
			outbuf.append("</div>\n");
			outbuf.append("<div class=\"infobox-content\">\n");
			outbuf.append("Your configuration changes were applied successfully<br />\n");
		} else {
			outbuf.append("<div class=\"infobox infobox-error\">\n");
			outbuf.append("<div class=\"infobox-header\">\n");
			outbuf.append("Configuration Could Not Be Applied\n");
			outbuf.append("</div>\n");
			outbuf.append("<div class=\"infobox-content\">\n");
			outbuf.append("Your configuration changes were applied with the following exceptions:<br />\n");
			outbuf.append(HTMLEncoder.encode(errbuf.toString()));
			outbuf.append("<br />\n");
		}
		
		outbuf.append("<a href=\".\" title=\"Configuration\">Return to Node Configuration</a><br />\n");
		outbuf.append("<a href=\"/\" title=\"Node Homepage\">Homepage</a>\n");
		
		outbuf.append("</div>\n");
		outbuf.append("</div>\n");
		
		ctx.getPageMaker().makeTail(outbuf);
		writeReply(ctx, 200, "text/html", "OK", outbuf.toString());
		
	}
	
	public void handleGet(URI uri, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		StringBuffer buf = new StringBuffer(1024);
		SubConfig[] sc = config.getConfigs();
		
		//HTTPRequest request = new HTTPRequest(uri);
		ctx.getPageMaker().makeHead(buf, "Freenet Node Configuration");

		buf.append("<div class=\"infobox infobox-normal\">\n");
		buf.append("<div class=\"infobox-header\">\n");
		buf.append("Freenet Node Configuration\n");
		buf.append("</div>\n");
		buf.append("<div class=\"infobox-content\">\n");
		buf.append("<form method=\"post\" action=\".\">");
		buf.append("<input type=\"hidden\" name=\"formPassword\" value=\""+node.formPassword+"\">");
		
		//String last = null;
		
		for(int i=0; i<sc.length;i++){
			Option[] o = sc[i].getOptions();
			//String prefix = new String(sc[i].getPrefix());
			
			/*
			if(last == null || ! last.equalsIgnoreCase(prefix)){
				//buf.append("</p>\n");
				buf.append("</span>\n");
				buf.append("<span id=\""+prefix+"\">\n");
				//buf.append("<p>\n");
			}
			*/
			
			buf.append("<ul class=\"config\"><span class=\"configprefix\">"+sc[i].getPrefix()+"</span>\n");
			
			for(int j=0; j<o.length; j++){
				String configName = o[j].getName();
				
				buf.append("<li>");
				//
				buf.append("<span class=\"configshortdesc\">");
				buf.append(o[j].getShortDesc());
				buf.append("</span>");	
				buf.append("<span class=\"config\">");
				//
				if(o[j].getValueString().equals("true") || o[j].getValueString().equals("false")){
					buf.append("<select name=\""+sc[i].getPrefix()+"."+configName+"\" >");
					if(o[j].getValueString().equals("true")){
						buf.append("<option value=\"true\" selected>true</option>");
						buf.append("<option value=\"false\">false</option>");
					}else{
						buf.append("<option value=\"true\">true</option>");
						buf.append("<option value=\"false\" selected>false</option>");
					}
					buf.append("</select>");
				}else{
					buf.append("<input alt=\""+o[j].getShortDesc()+"\" class=\"config\"" +
							" type=\"text\" name=\""+sc[i].getPrefix()+"."+configName+"\" value=\""+HTMLEncoder.encode(o[j].getValueString())+"\" />");				
				}
				buf.append("</span>");
				buf.append("<span class=\"configlongdesc\">");
				buf.append(o[j].getLongDesc());
				buf.append("</span>");
				
				buf.append("</li>\n");
			}
			
			
			buf.append("</ul>\n");
		}
		
		buf.append("<input type=\"submit\" value=\"Apply\" />");
		buf.append("<input type=\"reset\" value=\"Reset\" />");
		buf.append("</form>");
		buf.append("</div>\n");
		buf.append("</div>\n");
		
		ctx.getPageMaker().makeTail(buf);
		
		this.writeReply(ctx, 200, "text/html", "OK", buf.toString());
	}
	
	public void handlePut(URI uri, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		StringBuffer buf = new StringBuffer();
		buf.append("ok!\n");
		buf.append(uri);
		this.writeReply(ctx, 200, "text/html", "OK", buf.toString());
	}
	
	public String supportedMethods() {
		return "GET, PUT";
	}
}
