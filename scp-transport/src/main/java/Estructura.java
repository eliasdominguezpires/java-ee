import java.math.BigDecimal;

public class Estructura {

	private Long id;
	private String CDC;
	private String estado;
	private BigDecimal timbrado;
	private String numeroComprobante;
	private String numeroLote;
	private String respuesta;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCDC() {
		return CDC;
	}

	public void setCDC(String cDC) {
		CDC = cDC;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public BigDecimal getTimbrado() {
		return timbrado;
	}

	public void setTimbrado(BigDecimal timbrado) {
		this.timbrado = timbrado;
	}

	public String getNumeroComprobante() {
		return numeroComprobante;
	}

	public void setNumeroComprobante(String numeroComprobante) {
		this.numeroComprobante = numeroComprobante;
	}

	public String getNumeroLote() {
		return numeroLote;
	}

	public void setNumeroLote(String numeroLote) {
		this.numeroLote = numeroLote;
	}

	public String getRespuesta() {
		return respuesta;
	}

	public void setRespuesta(String respuesta) {
		this.respuesta = respuesta;
	}
}
