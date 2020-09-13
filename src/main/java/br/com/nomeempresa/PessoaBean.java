package br.com.nomeempresa;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;

import br.com.dao.DaoGeneric;
import br.com.entidades.Cidades;
import br.com.entidades.Estados;
import br.com.entidades.Pessoa;
import br.com.jpautil.JPAUtil;
import br.com.repository.IDaoPessoa;

@ViewScoped // a anotação aqui é igual à anterior, mas o import é diferente (esse é do CDI)
@Named(value="pessoaBean")
public class PessoaBean implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Inject
	private JPAUtil jpaUtil;

	private Pessoa pessoa = new Pessoa();
	private List<Pessoa> pessoas = new ArrayList<Pessoa>();

	@Inject
	private DaoGeneric<Pessoa> daoGeneric;

	@Inject
	private IDaoPessoa iDaoPessoa; /* Veja que não estamos mais criando os objetos na mão,
	ou seja, instanciando ele, apenas colocamos o Inject */
	
	private List<SelectItem> estados;

	private List<SelectItem> cidades;

	private Part arquivofoto;

	public Part getArquivofoto() {
		return arquivofoto;
	}

	public void setArquivofoto(Part arquivofoto) {
		this.arquivofoto = arquivofoto;
	}

	public List<SelectItem> getCidades() {
		return cidades;
	}

	public void setCidades(List<SelectItem> cidades) {
		this.cidades = cidades;
	}

	public String salvar() throws IOException {
		// Processando imagem
		byte[] imagemByte = getByte(arquivofoto.getInputStream());
		pessoa.setFotoIconBase64Original(imagemByte); // Salva imagem original

		// Transformando em bufferedImage
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imagemByte));

		// Pegando o tipo da imagem
		int type = bufferedImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType();
		int largura = 200;
		int altura = 200;

		// Criando a nossa miniatura
		BufferedImage resizedImage = new BufferedImage(largura, altura, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(bufferedImage, 0, 0, largura, altura, null);
		g.dispose(); // Grava a imagem

		// Escrever novamente a imagem em tamanho menor
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String extensao = arquivofoto.getContentType().split("\\/")[1]; // Retorna image/png
		ImageIO.write(resizedImage, extensao, baos);

		String miniImagem = "data:" + arquivofoto.getContentType() + ";base64,"
				+ DatatypeConverter.printBase64Binary(baos.toByteArray());

		// Processar imagem
		pessoa.setFotoIconBase64(miniImagem);
		pessoa.setExtensao(extensao);

		pessoa = daoGeneric.merge(pessoa);
		carregarPessoas();
		mostrarMsg("Cadastrado com sucesso!!");
		return "";
	}

	// Método receita de bolo para converter um inputStream de um arquivo para array
	// de bytes
	private byte[] getByte(InputStream is) throws IOException {
		int length;
		int size = 1024;
		byte[] buf = null;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			length = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];

			while ((length = is.read(buf, 0, size)) != -1) {
				bos.write(buf, 0, length);
			}

			buf = bos.toByteArray();
		}
		return buf;
	}

	private void mostrarMsg(String msg) {

		FacesContext context = FacesContext.getCurrentInstance();
		FacesMessage message = new FacesMessage(msg);
		context.addMessage(null, message);
	}

	public String novo() {
		pessoa = new Pessoa();
		return "";
	}

	public String limpar() {
		pessoa = new Pessoa();
		return "";
	}

	public String remove() {
		daoGeneric.deletePorId(pessoa);
		pessoa = new Pessoa();
		carregarPessoas();
		mostrarMsg("Removido com sucesso!!");
		return "";
	}

	public void pesquisaCep(AjaxBehaviorEvent event) {
		try {
			URL url = new URL("https://viacep.com.br/ws/" + pessoa.getCep() + "/json/");

			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			String cep = "";
			StringBuilder jsonCep = new StringBuilder();

			while ((cep = br.readLine()) != null) {
				jsonCep.append(cep);
			}

			Pessoa gsonAux = new Gson().fromJson(jsonCep.toString(), Pessoa.class);

			pessoa.setCep(gsonAux.getCep());
			pessoa.setLogradouro(gsonAux.getLogradouro());
			pessoa.setComplemento(gsonAux.getComplemento());
			pessoa.setBairro(gsonAux.getBairro());
			pessoa.setLocalidade(gsonAux.getLocalidade());
			pessoa.setUf(gsonAux.getUf());
			pessoa.setUnidade(gsonAux.getUnidade());
			pessoa.setIbge(gsonAux.getIbge());
			pessoa.setGia(gsonAux.getGia());

		} catch (Exception e) {
			e.printStackTrace();
			mostrarMsg("Erro ao consultar cep");
		}
	}

	@PostConstruct
	public void carregarPessoas() {
		pessoas = daoGeneric.getListEntity(Pessoa.class);
	}

	public Pessoa getPessoa() {
		return pessoa;
	}

	public void setPessoa(Pessoa pessoa) {
		this.pessoa = pessoa;
	}

	public DaoGeneric<Pessoa> getDaoGeneric() {
		return daoGeneric;
	}

	public void setDaoGeneric(DaoGeneric<Pessoa> daoGeneric) {
		this.daoGeneric = daoGeneric;
	}

	public List<Pessoa> getPessoas() {
		return pessoas;
	}

	public String deslogar() {

		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		externalContext.getSessionMap().remove("usuarioLogado");

		@SuppressWarnings("static-access")
		HttpServletRequest httpServletRequest = (HttpServletRequest) context.getCurrentInstance().getExternalContext()
				.getRequest();

		httpServletRequest.getSession().invalidate();

		return "index.jsf";
	}

	public String logar() {

		Pessoa pessoaUser = iDaoPessoa.consultarUsuario(pessoa.getLogin(), pessoa.getSenha());

		if (pessoaUser != null) {

			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			externalContext.getSessionMap().put("usuarioLogado", pessoaUser);

			return "primeirapagina.jsf";
		}
		return "index.jsf";
	}

	public boolean permiteAcesso(String acesso) {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		Pessoa pessoaUser = (Pessoa) externalContext.getSessionMap().get("usuarioLogado");

		return pessoaUser.getPerfilUser().equals(acesso);
	}

	public List<SelectItem> getEstados() {
		estados = iDaoPessoa.listaEstados();
		return estados;
	}

	public void carregaCidades(AjaxBehaviorEvent event) {

		Estados estado = (Estados) ((HtmlSelectOneMenu) event.getSource()).getValue();

		if (estado != null) {
			pessoa.setEstados(estado);

			@SuppressWarnings("unchecked")
			List<Cidades> cidades = jpaUtil.getEntityManager()
					.createQuery("from Cidades where estados.id = " + estado.getId()).getResultList();

			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();

			for (Cidades cidade : cidades) {
				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}
			setCidades(selectItemsCidade);
		}
	}

	@SuppressWarnings("unchecked")
	public String editar() {
		if (pessoa.getCidades() != null) {
			Estados estado = pessoa.getCidades().getEstados();
			pessoa.setEstados(estado);

			List<Cidades> cidades = jpaUtil.getEntityManager()
					.createQuery("from Cidades where estados.id = " + estado.getId()).getResultList();

			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();

			for (Cidades cidade : cidades) {
				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}
			setCidades(selectItemsCidade);

		}
		return "";
	}

	public void download() throws IOException {
		Map<String, String> params = FacesContext.getCurrentInstance()
				.getExternalContext().getRequestParameterMap();

		String fileDownloadId = params.get("fileDownloadId");

		Pessoa pessoa = daoGeneric.consultar(Pessoa.class, fileDownloadId);

		HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
				.getExternalContext().getResponse();
		
		response.addHeader("Content-Disposition", "attachment; filename=download." 
		+ pessoa.getExtensao()); // Setando o cabeçalho, dizendo o tipo de arquivo

		response.setContentType("application/octet-stream"); //stream é relativo a mídia, foto
		response.setContentLength(pessoa.getFotoIconBase64Original().length); // Tamanho
		response.getOutputStream().write(pessoa.getFotoIconBase64Original()); // Seta os dados
		response.getOutputStream().flush(); // confirma a resposta do fluxo de dados
		FacesContext.getCurrentInstance().responseComplete(); // Diz que é a resposta completa
				
	}
	
	public void mudancaDeValor(ValueChangeEvent evento) {
		System.out.println("Valor antigo: " + evento.getOldValue());
		System.out.println("Valor novo: " + evento.getNewValue());
	}

}
