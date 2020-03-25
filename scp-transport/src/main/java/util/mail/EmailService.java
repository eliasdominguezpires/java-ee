package util.mail;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailService implements Callable{

	private Session mailSession;
	
	public static String CONTENT_TEXT;

	public EmailService(Session mailSession){
		this.mailSession = mailSession;
		EmailService.CONTENT_TEXT="text/plain; charset=ISO-8859-1";
	}
	
	/**
	 * Método para el envío de email a una lista de lista de destinatarios, una lista de destinatarios con copia, 
	 * una lista de destinatario con copia oculta, con asunto, con cuerpo del email y lista de adjuntos
	 * 
	 * @param to								lista de destinatarios
	 * @param cc								lista de destinatarios con copia
	 * @param cco								lista de destinatario con copia oculta
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @param attachments						lista de adjuntos
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to, Set<String> cc, Set<String> cco, String subject, String messageText, Set<FileDataSource> attachments)
			throws Exception, IllegalArgumentException {
		
		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);
		
		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"
			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		// Recipientes "con copia"
		if (cc != null && !cc.isEmpty()) {

			InternetAddress[] recipientsCc = new InternetAddress[cc.size()];
			String[] recipientsCcArray = cc.toArray(new String[] {});
			for (int i = 0; i < cc.size(); i++) {
				if (isValidEmailAddress(recipientsCcArray[i])){
					recipientsCc[i] = new InternetAddress(recipientsCcArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.CC, recipientsCc);
		}

		// Recipientes "con copia oculta"
		if (cco != null && !cco.isEmpty()) {

			InternetAddress[] recipientsCco = new InternetAddress[cco.size()];
			String[] recipientsCcoArray = cco.toArray(new String[] {});
			for (int i = 0; i < cco.size(); i++) {
				if (isValidEmailAddress(recipientsCcoArray[i])){
					recipientsCco[i] = new InternetAddress(recipientsCcoArray[i]);
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcoArray[i] +"> no es válida");
				}
			}

			// BCC significa Blind Carbon Copy y es el equivalente a Copia
			// Oculta
			message.addRecipients(Message.RecipientType.BCC, recipientsCco);
		}
		
		// Agrega los adjuntos al email
		if (attachments != null && !attachments.isEmpty()) {
			for (FileDataSource file : attachments) {
				BodyPart adjuntos = new MimeBodyPart();
				adjuntos.setDataHandler(new DataHandler((FileDataSource) file));
				adjuntos.setFileName(((FileDataSource) file).getName());
				multiParte.addBodyPart(adjuntos);
			}
		}

		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}
	
	/**
	 * Método para el envío de email a una lista de lista de destinatarios, una lista de destinatarios con copia, 
	 * una lista de destinatario con copia oculta, con asunto y cuerpo del email
	 * 
	 * @param to								lista de destinatarios
	 * @param cc								lista de destinatarios con copia
	 * @param cco								lista de destinatario con copia oculta
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to, Set<String> cc, Set<String> cco, String subject, String messageText)
			throws Exception, IllegalArgumentException {
		
		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);
		
		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"
			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		// Recipientes "con copia"
		if (cc != null && !cc.isEmpty()) {

			InternetAddress[] recipientsCc = new InternetAddress[cc.size()];
			String[] recipientsCcArray = cc.toArray(new String[] {});
			for (int i = 0; i < cc.size(); i++) {
				if (isValidEmailAddress(recipientsCcArray[i])){
					recipientsCc[i] = new InternetAddress(recipientsCcArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.CC, recipientsCc);
		}

		// Recipientes "con copia oculta"
		if (cco != null && !cco.isEmpty()) {

			InternetAddress[] recipientsCco = new InternetAddress[cco.size()];
			String[] recipientsCcoArray = cco.toArray(new String[] {});
			for (int i = 0; i < cco.size(); i++) {
				if (isValidEmailAddress(recipientsCcoArray[i])){
					recipientsCco[i] = new InternetAddress(recipientsCcoArray[i]);
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcoArray[i] +"> no es válida");
				}
			}

			// BCC significa Blind Carbon Copy y es el equivalente a Copia
			// Oculta
			message.addRecipients(Message.RecipientType.BCC, recipientsCco);
		}
		
		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}
	
	/**
	 * Método para el envío de email a una lista de lista de destinatarios, una lista de destinatarios con copia, 
	 * con asunto y cuerpo del email
	 * 
	 * @param to								lista de destinatarios
	 * @param cc								lista de destinatarios con copia
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to, Set<String> cc, String subject, String messageText)
			throws Exception, IllegalArgumentException {
		
		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);
		
		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"
			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		// Recipientes "con copia"
		if (cc != null && !cc.isEmpty()) {

			InternetAddress[] recipientsCc = new InternetAddress[cc.size()];
			String[] recipientsCcArray = cc.toArray(new String[] {});
			for (int i = 0; i < cc.size(); i++) {
				if (isValidEmailAddress(recipientsCcArray[i])){
					recipientsCc[i] = new InternetAddress(recipientsCcArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.CC, recipientsCc);
		}

		
		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}
	
	/**
	 * Método para el envío de email a una lista de lista de destinatarios, una lista de destinatario con copia oculta, 
	 * con asunto, con cuerpo del email y lista de adjuntos
	 * 
	 * @param to								lista de destinatarios
	 * @param cco								lista de destinatario con copia oculta
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @param attachments						lista de adjuntos
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to, Set<String> cco, String subject, String messageText, Set<FileDataSource> attachments)
			throws Exception, IllegalArgumentException {
		
		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);
		
		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"
			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		// Recipientes "con copia oculta"
		if (cco != null && !cco.isEmpty()) {

			InternetAddress[] recipientsCco = new InternetAddress[cco.size()];
			String[] recipientsCcoArray = cco.toArray(new String[] {});
			for (int i = 0; i < cco.size(); i++) {
				if (isValidEmailAddress(recipientsCcoArray[i])){
					recipientsCco[i] = new InternetAddress(recipientsCcoArray[i]);
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsCcoArray[i] +"> no es válida");
				}
			}

			// BCC significa Blind Carbon Copy y es el equivalente a Copia
			// Oculta
			message.addRecipients(Message.RecipientType.BCC, recipientsCco);
		}
		
		// Agrega los adjuntos al email
		if (attachments != null && !attachments.isEmpty()) {
			for (FileDataSource file : attachments) {
				BodyPart adjuntos = new MimeBodyPart();
				adjuntos.setDataHandler(new DataHandler((FileDataSource) file));
				adjuntos.setFileName(((FileDataSource) file).getName());
				multiParte.addBodyPart(adjuntos);
			}
		}

		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}

	/**
	 * Método para el envío de email a una lista de lista de destinatarios, 
	 * con asunto, con cuerpo del email y lista de adjuntos
	 * 
	 * @param to								lista de destinatarios
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @param attachments						lista de adjuntos
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to,	String subject, String messageText, Set<FileDataSource> attachments)
			throws Exception, IllegalArgumentException {

		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);

		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"


			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		// Agrega los adjuntos al email
		if (attachments != null && !attachments.isEmpty()) {
			for (FileDataSource file : attachments) {
				BodyPart adjuntos = new MimeBodyPart();
				adjuntos.setDataHandler(new DataHandler((FileDataSource) file));
				adjuntos.setFileName(((FileDataSource) file).getName());
				multiParte.addBodyPart(adjuntos);
			}
		}

		//message.setFrom(new InternetAddress("facturaelectronica@kiga.com.py"));
		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}

	/**
	 * Método para el envío de email a una lista de lista de destinatarios, 
	 * con asunto y con cuerpo del email
	 * 
	 * @param to								lista de destinatarios
	 * @param subject							asunto
	 * @param messageText						cuerpo del email
	 * @return									retorna si fue exitoso el envío
	 * 
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si no se especifica un destinatario
	 * @throws IllegalArgumentException			progapa una {@code IllegalArgumentException} si alguna de las direcciones de email esta mal formada
	 * @throws IllegalArgumentException			propaga una {@code IllegalArgumentException} si se especificó una dirección de email inválida
	 * @throws Exception						propaga una {@code Exception} si no se pudo autenticar la sesión de email
	 * @throws Exception						propaga una {@code Exception} si por algón motivo lanza un {@code Exception} no contemplado
	 */
	public String sendEmail(Set<String> to, String subject, String messageText)
			throws Exception, IllegalArgumentException {
		
		MimeMultipart multiParte = new MimeMultipart();
		BodyPart contenido = new MimeBodyPart();
		contenido.setContent(messageText, EmailService.CONTENT_TEXT);
		
		multiParte.addBodyPart(contenido);
		
		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));

		if (to == null || to.isEmpty()) {
			throw new IllegalArgumentException("Se requieren los destinatarios");
		} else {

			// Recipientes "destino"
			InternetAddress[] recipientsTo = new InternetAddress[to.size()];
			String[] recipientsToArray = to.toArray(new String[] {});
			for (int i = 0; i < to.size(); i++) {
				if (isValidEmailAddress(recipientsToArray[i])){
					recipientsTo[i] = new InternetAddress(recipientsToArray[i]);					
				}else{
					throw new IllegalArgumentException("La dirección de email <"+ recipientsToArray[i] +"> no es válida");
				}
			}

			message.addRecipients(Message.RecipientType.TO, recipientsTo);
		}

		message.setContent(multiParte);
		message.setSubject(subject);
		message.setSentDate(new Date());
		
		try{
			Transport.send(message,message.getAllRecipients());
			return "Email Enviado";
		}catch (SendFailedException sfex){
			throw new IllegalArgumentException  ("La dirección de correo eletrónico está mal formada.");
		}catch (AddressException aex){
			throw new IllegalArgumentException("No se especificó una dirección de correo electrónico valida.");
		} catch (AuthenticationFailedException afex) {
			throw new Exception ("Se produjo un error de Autenticación.");						
		}catch (Exception ex) {
			throw new Exception (ex.getMessage());
	}
}

	/**
	 * Metodo que verifica la validad de un email
	 * de acuerdo al atributo especificado
	 * @param email
	 * @return
	 */
	public static boolean isValidEmailAddress(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

	@Override
	public Object call() throws Exception {
		return null;
	}
}
