import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class Engine {

	private HashMap<String, DpiUtility> m_mapDpiUtility;
	private HashMap<String, PluginUtility> m_mapPluginUtility;
	private DpiUtility m_currentDpiUtility;
	private Class<?> m_classPlugin;
	private Object m_objectPlugin;
	
	public Engine(String coreConfigXml, String pluginsConfigXml) throws DocumentException, IOException{
		setCoreConfiguration(coreConfigXml);
		setPluginsConfiguration(pluginsConfigXml);
	}
	
	public void setCoreConfiguration(String coreConfigXml) throws DocumentException{
		m_mapDpiUtility = new HashMap<String, DpiUtility>();
		InputStream inputXml = this.getClass().getResourceAsStream(coreConfigXml);
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(inputXml);
        Element configuration = document.getRootElement();
        // 读取DPI配置
        List<Element> dpiList = configuration.elements("dpi");		// DPI类型列表
        for(Element dpi : dpiList){
        	String name = dpi.attributeValue("name");
        	String source = dpi.attributeValue("source");
        	String seperator = dpi.attributeValue("seperator");
        	DpiUtility desc = new DpiUtility(name, source, seperator); // ======
        	List<Element> fieldList = dpi.elements("field");
        	for(Element field : fieldList){
        		String fieldname = field.attributeValue("name");
        		String index = field.attributeValue("index");
        		String encode = field.attributeValue("encode");
        		String type = field.getTextTrim();
        		desc.setFieldIndex(fieldname, index, encode, type); // =========
        	}
        	m_mapDpiUtility.put(name, desc);
        }
	}
	
	public void setPluginsConfiguration(String pluginsConfigXml) throws DocumentException{
		m_mapPluginUtility = new HashMap<String, PluginUtility>();
		InputStream inputXml = this.getClass().getResourceAsStream(pluginsConfigXml);
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(inputXml);
        Element plugins = document.getRootElement();
        String rootDirectory = plugins.attributeValue("root");
        // 读取Plugin配置
        List<Element> pluginList = plugins.elements("plugin");	
        for(Element plugin : pluginList){
        	String name = plugin.attributeValue("name");
        	String filename = plugin.element("filename").getTextTrim();
        	String extension = plugin.element("extension").getTextTrim();
        	String entryclass = plugin.element("entryclass").getTextTrim();
        	PluginUtility desc = new PluginUtility(rootDirectory, name, filename, extension, entryclass);
        	m_mapPluginUtility.put(name, desc);
        }
	}
	
	public void setDpi(String dpiName){
		DpiUtility dpi = m_mapDpiUtility.get(dpiName);
		m_currentDpiUtility = dpi;
	}
	
	public void loadPlugin(String pluginName) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, URISyntaxException{
		PluginUtility plugin = m_mapPluginUtility.get(pluginName);
		String strPluginJarPath = plugin.m_strRoot + "/" + plugin.m_strFileName + "." + plugin.m_strExtension;
		File filePluginJar = File.createTempFile(plugin.m_strFileName, "." + plugin.m_strExtension);
		filePluginJar.deleteOnExit();
		InputStream in = getClass().getResourceAsStream(strPluginJarPath);
		OutputStream out = new FileOutputStream(filePluginJar);
		IOUtils.copy(in, out);
		in.close();
		out.close();
		registerPlugin(filePluginJar.toURI().toURL(), plugin.m_strEntryClass);
	}
	
	private void registerPlugin(URL url, String entry) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		URLClassLoader loader = new URLClassLoader(new URL[]{ url });
		m_classPlugin = loader.loadClass(entry);
		m_objectPlugin = m_classPlugin.newInstance();
	}
	
	
	public void tagging(String line) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		Method methodTagging = m_classPlugin.getMethod("tagging", String.class, String.class, List.class);
		methodTagging.invoke(m_objectPlugin, line, m_currentDpiUtility.m_strSeperator, m_currentDpiUtility.m_listFields);
	}
}
