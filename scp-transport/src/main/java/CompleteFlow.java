import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import basic.ssh.SSHManager;
import util.mail.Mail;

/**
 * @author elias dominguez pires
 */
public class CompleteFlow {	

	private static Properties properties;		
	
	private final static Logger LOG = Logger.getLogger(CompleteFlow.class.getName());
	private static FileHandler fh = null;
	
	/**
	 * 
	 * @return
	 */
	public static Properties readPropertiesDataBase() {			
		
        Properties props = new Properties();
        Path myPath = Paths.get("src/main/java/database.properties");

        try {
        	//Config LOG
        	fh=new FileHandler("logger.xml", true);			
    		LOG.addHandler(fh);
    		
            BufferedReader bf = Files.newBufferedReader(myPath, 
                StandardCharsets.UTF_8);

            props.load(bf);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return props;
    }
	
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {		
		
		//Leer archivo de configuracion    
		String rootPath = System.getProperty("user.dir") + "/src/main/java/";		
		String appPropertiesPath = rootPath + "config.properties";
		try {	
			//Cargar propiedades			
			Properties props = readPropertiesDataBase();														
			properties = new Properties();
			properties.load(new FileInputStream(appPropertiesPath));
					
			//path servidor remoto		  		
			String remoteB = properties.getProperty("host.remoto.path");		
			
			// path de los archivos a leer para copiar, eliminar		
			String path = properties.getProperty("path.csv.dir");		
			String pathBakup = properties.getProperty("path.csv.backup.dir");		
	
			//Configuracion server 
			String userName = properties.getProperty("conf.server.userName");		
			String host = properties.getProperty("conf.server.host");		
			String keyFilePath = System.getProperty("user.dir") + properties.getProperty("path.keyFile");				
			
			String keyPassword = null; 
			int timeOut = 60000;									
			
			LOG.log(Level.INFO,"Configuraciones HOST - PATH \n path: " + path + " pathBackup:" +pathBakup + "\n UserName:" +userName 
					+ " host: " +host);										
			
			SSHManager instance = new SSHManager(userName, host, 22, keyFilePath, keyPassword, timeOut);
			Session jschSession = instance.connect();

			// copiar del remoto al local
			//copyRemoteToLocal(jschSession, PATHremoteA + fileRemoto, PAHTlocal);

			// eliminar archivos del remotos
			//String command = "rm " + PATHremoteA + fileEliminarRemoto;
			//instance.sendCommand(command);			
			
			//Crear Backup
			FileUtils.copyDirectory(new File(path), new File(pathBakup + "/"+LocalDate.now()+
						"-"+LocalDateTime.now().getHour()+LocalDateTime.now().getMinute()));			
			LOG.log(Level.INFO, "Copy Files OK");
			
			// copiar del local al remoto			
			List<File> filesInFolder = Files.list(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile) 
                    .collect(Collectors.toList());
			
			for (File file : filesInFolder) {				
				if(file.isFile())
					copyLocalToRemote(jschSession, path+file.getName(), remoteB);
				
				LOG.log(Level.INFO, "Archivo Copiado al remoto :\t" + file.getPath());
			}
						
			//Cerrar instancia SSH
			instance.close();
			
			//Eliminar Archivos
			Path pathDelete = Paths.get(path);
			Files.list(pathDelete).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			//System.out.println("Delete Files Ok !");
			LOG.log(Level.INFO, "Delete Files Ok !");
			
			// Llamar al servicio REST			
			try {				
				//URL from Data								
				URL url = new URL("http://test.kigafe.com:8080/qualita-facturadigital/rest/comprobantes/csvsaync");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();				
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Accept", "application/json");
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP Error code : " + conn.getResponseCode());
				}
				InputStreamReader in = new InputStreamReader(conn.getInputStream());
				BufferedReader br = new BufferedReader(in);
				String output;
				while ((output = br.readLine()) != null) {
					//System.out.println(output);
					LOG.log(Level.INFO, output);
				}
				conn.disconnect();				
			} catch (Exception e) {				
				// si ocurre una exepcion enviar mail para que se ejecute manualmente con el error
				LOG.log(Level.SEVERE, "Exception in llamado al Service Rest:- ",e);
				try {
					List<String> destinatario = new ArrayList<>();
					destinatario.add("mail.destinatario");
					enviarMail(destinatario, null, "Trasnferencia de Archivos", "Error excepcion Service REST "+ e, null);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					LOG.log(Level.SEVERE, "Exception in llamado al Service Rest:- ",e1);
				}
			}					
			
			List<Estructura> list = new ArrayList<Estructura>();
			int cantidadInsert = 0;
			int cantidadUpdate = 0;
			int cantidadDelete = 0;
			
			//actualizar estados de las facturas procesadas
			System.out.println("\nPOSTGRES \n");															
	        String url = props.getProperty("db.postgresKiga.url");
	        String user = props.getProperty("db.postgresKiga.user");
	        String passwd = props.getProperty("db.postgresKiga.passwd");
	        try (Connection con = DriverManager.getConnection(url, user, passwd); //estados_postgres
	                PreparedStatement pst = con.prepareStatement("SELECT * FROM fe_fact_estado");
	                ResultSet rs = pst.executeQuery()) {	        	
	            while (rs.next()) {	            		            		           
	            	Estructura object = new Estructura();
	            	object.setId(rs.getLong(1));
	            	object.setCDC(rs.getString(2));
	            	object.setEstado(rs.getString(3));
	            	object.setNumeroComprobante(rs.getString(4));
	            	object.setRespuesta(rs.getString(5));
	            	object.setTimbrado(rs.getBigDecimal(6));
	            	object.setNumeroLote(rs.getString(7));	            	
	            		            	
	            	list.add(object);

	            	System.out.println(rs.getLong(1)+" "+rs.getString(2)+" "+rs.getString(3)+" "+rs.getString(4)+" "+rs.getBigDecimal(6)+" "+rs.getString(7));
	            }	            
	        } catch (SQLException ex) {	            
	            LOG.log(Level.SEVERE, ex.getMessage(), ex);
	        }
			
	        //verificar estado de nuestro sistema
			System.out.println("\nORACLE \n");
			url = props.getProperty("db.oracle.url");
	        user = props.getProperty("db.oracle.user");
	        passwd = props.getProperty("db.oracle.passwd");
	        Class.forName("oracle.jdbc.driver.OracleDriver");
			try (Connection con = DriverManager.getConnection(url,user,passwd);){				
				for (Estructura estructura : list) {
					//hacer select de la table y ver si se interta o se actualiza la tabla.
					PreparedStatement stmt = con.prepareStatement("select * from ORACLE.ESTADOS where TIMBRADO=? "
							+ " AND NUMERO_COMPROBANTE =?"
							+ " AND ESTADO =?");
					stmt.setBigDecimal(1, estructura.getTimbrado());					
					stmt.setString(2, estructura.getNumeroComprobante());
					stmt.setString(3, estructura.getEstado());
					ResultSet rs = stmt.executeQuery();
					
					PreparedStatement stmt2 = con.prepareStatement("select * from ORACLE.ESTADOS where TIMBRADO=? "							
							+ " AND NUMERO_COMPROBANTE =?"
							+ " AND ESTADO !=?");
					stmt2.setBigDecimal(1, estructura.getTimbrado());					
					stmt2.setString(2, estructura.getNumeroComprobante());
					stmt2.setString(3, estructura.getEstado());
					ResultSet rs2 = stmt2.executeQuery();
					
					if(rs2.next() && !rs.next()) {
						//actualizar
						PreparedStatement stmtUP = con.prepareStatement("update ORACLE.ESTADOS set ESTADO=?, respuesta_lote=?,"
								+ " CDC= ?, numero_lote=?"								
								+ " where TIMBRADO=?"
								+ " and NUMERO_COMPROBANTE =?"
								+ " AND ESTADO != 'Aprobado'");
						//SET
						stmtUP.setString(1,	estructura.getEstado());
						stmtUP.setString(2, estructura.getRespuesta());
						stmtUP.setString(3, estructura.getCDC());
						stmtUP.setString(4, estructura.getNumeroLote());
						//WHERE						
						stmtUP.setBigDecimal(5, estructura.getTimbrado());
						stmtUP.setString(6, estructura.getNumeroComprobante());
						cantidadUpdate += stmtUP.executeUpdate();							
					
						
					}else if(!rs.next() && !rs2.next()){
						PreparedStatement stmt3 = con.prepareStatement("select * from ORACLE.ESTADOS where NUMERO_COMPROBANTE=? and timbrado=?");
						stmt3.setString(1, estructura.getNumeroComprobante());						
						stmt3.setBigDecimal(2, estructura.getTimbrado());
						ResultSet rs3 = stmt3.executeQuery();
						if(!rs3.next()){
							//insert
							PreparedStatement stmtInsert = con.prepareStatement("insert into ORACLE.ESTADOS values(?,?,?,?,?,?,?)");
							stmtInsert.setLong(1,	estructura.getId());  
							stmtInsert.setString(2,	estructura.getCDC());  
							stmtInsert.setString(3, estructura.getEstado());  
							stmtInsert.setBigDecimal(4, estructura.getTimbrado());
							stmtInsert.setString(5, estructura.getNumeroComprobante());					
							stmtInsert.setString(6, estructura.getNumeroLote());
							stmtInsert.setString(7, estructura.getRespuesta());
							cantidadInsert += stmtInsert.executeUpdate();																		
						}
					}					  				
				}			
												
				PreparedStatement stmt=con.prepareStatement("select * from ORACLE.ESTADOS");  
				ResultSet rs=stmt.executeQuery();  
				
				while(rs.next()){  
					System.out.println(rs.getLong(1)+" "+rs.getString(2)+" "+rs.getString(3)+" "+rs.getString(5)+" "+rs.getBigDecimal(4)+" "+rs.getString(6));  
				}				
				con.close();
				//Mover cuando ocurra un error
				//Eliminar Registros Postgres			
				url = props.getProperty("db.postgresKiga.url");
		        user = props.getProperty("db.postgresKiga.user");
		        passwd = props.getProperty("db.postgresKiga.passwd");
		        try (Connection conPost = DriverManager.getConnection(url, user, passwd);
		        		){
		        	for (Estructura estructura : list) {
		        		if(!"Enviado".equals(estructura.getEstado()))
		        		{		        			
		        			PreparedStatement pst = conPost.prepareStatement("delete from fe_fact_estado where timbrado = ? and estado = ? and numerocomprobante = ?");
		        			pst.setBigDecimal(1, estructura.getTimbrado());
		        			pst.setString(2, estructura.getEstado());
		        			pst.setString(3, estructura.getNumeroComprobante());
		        			cantidadDelete += pst.executeUpdate();
		        		}
		        	}		        			        
		        	conPost.close();
				} catch (Exception e) {					
					LOG.log(Level.SEVERE, "Exception in delete from data base Postgres:- ",e);
				}
			} catch (Exception e) {				
				LOG.log(Level.SEVERE, "Exception al intentar optener estados :- ",e);
			}
			
			LOG.log(Level.INFO, cantidadInsert+" records inserted");
			LOG.log(Level.INFO, cantidadUpdate+" records updated");
			LOG.log(Level.INFO, cantidadDelete+" records deleted");
			LOG.log(Level.INFO, "Done !");
		} catch (IOException | IllegalArgumentException e) {			
			LOG.log(Level.SEVERE, "Exception in llamado al Service Rest:- ",e);
			//enviar mail, hacer rolback de todo			
			List<String> destinatario = new ArrayList<>();
			destinatario.add("mail.destinatario");				
			try {
				enviarMail(destinatario, null, "Trasnferencia de Archivos", "Error excepcion General "+ e, null);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				LOG.log(Level.SEVERE, "Exception in llamado al Service Rest:- ",e1);
			}					
		}
	}

	public static void copyRemoteToLocal(Session session, String from, String to) throws JSchException, IOException {

		String prefix = null;
		if (new File(to).isDirectory()) {
			prefix = to + File.separator;
		}

		// exec 'scp -f rfile' remotely
		String command = "scp -f " + from;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		channel.connect();

		byte[] buf = new byte[1024];

		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();

		while (true) {
			int c = checkAck(in);
			if (c != 'C') {
				break;
			}

			// read '0644 '
			in.read(buf, 0, 5);

			long filesize = 0L;
			while (true) {
				if (in.read(buf, 0, 1) < 0) {
					// error
					break;
				}
				if (buf[0] == ' ')
					break;
				filesize = filesize * 10L + (long) (buf[0] - '0');
			}

			String file = null;
			for (int i = 0;; i++) {
				in.read(buf, i, 1);
				if (buf[i] == (byte) 0x0a) {
					file = new String(buf, 0, i);
					break;
				}
			}

			System.out.println("file-size=" + filesize + ", file=" + file);

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			// read a content of lfile
			FileOutputStream fos = new FileOutputStream(prefix == null ? to : prefix + file);
			int foo;
			while (true) {
				if (buf.length < filesize) 
					foo = buf.length;
				else
					foo = (int) filesize;
				foo = in.read(buf, 0, foo);
				if (foo < 0) {
					// error
					break;
				}
				fos.write(buf, 0, foo);
				filesize -= foo;
				if (filesize == 0L)
					break;
			}

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			try {
				if (fos != null)
					fos.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		channel.disconnect();
	}

	public static void copyLocalToRemote(Session session, String from, String to) throws JSchException, IOException {
		boolean ptimestamp = true;

		// exec 'scp -t rfile' remotely
		String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + to;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		channel.connect();

		if (checkAck(in) != 0) {
			System.exit(0);
		}

		File _lfile = new File(from);

		if (ptimestamp) {
			command = "T" + (_lfile.lastModified() / 1000) + " 0";
			// The access time should be sent here,
			// but it is not accessible with JavaAPI ;-<
			command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
		}

		// send "C0644 filesize filename", where filename should not include '/'
		long filesize = _lfile.length();
		command = "C0644 " + filesize + " ";
		if (from.lastIndexOf('/') > 0) {
			command += from.substring(from.lastIndexOf('/') + 1);
		} else {
			command += from;
		}

		command += "\n";
		out.write(command.getBytes());
		out.flush();

		if (checkAck(in) != 0) {
			System.exit(0);
		}

		// send a content of lfile
		FileInputStream fis = new FileInputStream(from);
		byte[] buf = new byte[1024];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len); // out.flush();
		}

		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();

		if (checkAck(in) != 0) {
			System.exit(0);
		}
		out.close();

		try {
			if (fis != null)
				fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		channel.disconnect();
	}

	public static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
	
	/**
	 * Este m&eacute;todo inicia el proceso de enviar mail
	 * 
	 * @param destinatario:
	 *            el correo destinatario o una lista de correos
	 * @param conCopia:
	 *            correo o lista de correos al/los que se enviar&aacute;n copia
	 *            del correo
	 * @param copiaOculta:
	 *            correo o lista de correos ocultos
	 * @param asunto:
	 *            especifica el asunto del correo
	 * @param mensaje:
	 *            especifica el contenido del correo (cuerpo del correo)
	 * @param adjuntos:
	 *            archivos o contenido adjunto al correo
	 * @throws Exception
	 * @throws IllegalArgumentException
	 */

	public static String enviarMail(List<String> destinatario, List<String> conCopia, String asunto, String mensaje,
			List<FileDataSource> adjuntos) throws IllegalArgumentException, Exception {
		final Mail mail = new Mail();
		String msg = "";
		
		try {
			mail.setEmailId("elias.dominguezpires@gmail.com");
			mail.setSubject(asunto);
			mail.setMessage(mensaje);
			mail.setPassword("tdn4321");
			mail.setHost("smtp.efm.de");
			mail.setSender("no-reply@tdn.com.py");

			Properties properties = System.getProperties();
			properties.put("mail.smtp.host", mail.getHost());
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.socketFactory.port", "587");
			properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			properties.put("mail.smtp.port", "587");
			javax.mail.Session session = javax.mail.Session.getInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAurhentication() {
					return new PasswordAuthentication(mail.getSender(), mail.getPassword());
				}
			});

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mail.getSender()));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mail.getEmailId()));
			message.setSubject(mail.getSubject());
			message.setText(mail.getMessage());
			Transport.send(message, mail.getSender(), mail.getPassword());
			System.out.println("Mail Sent");			
		} catch (Exception ex) { 
			//ex.printStackTrace();
			//ex.getCause();
			throw new RuntimeException("Error while sending mail" + ex, ex);
		}
		return msg;
	}
	
	public static void copyDir(Path src, Path dest) throws IOException {
		Files.list(src)
				.forEach(source -> {
					try {
						Files.copy(source, dest.resolve(src.relativize(source)),
										StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
	}
}