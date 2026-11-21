package unioeste.casestudy.irpf.bo;

import java.io.Serializable;
import unioeste.geral.pessoa.bo.PessoaJuridica;

public class RendimentoPJ implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idRendimento;
    private Double valorTotalRecebido;
    private Double valor13Salario;
    private Double irrfPago;
    private Double inss;
    private PessoaJuridica fontePagadora;
    private Integer idDeclaracao;

    public RendimentoPJ() {}

    public Integer getIdRendimento() { return idRendimento; }
    public void setIdRendimento(Integer idRendimento) { this.idRendimento = idRendimento; }

    public Double getValorTotalRecebido() { return valorTotalRecebido; }
    public void setValorTotalRecebido(Double valorTotalRecebido) { this.valorTotalRecebido = valorTotalRecebido; }

    public Double getValor13Salario() { return valor13Salario; }
    public void setValor13Salario(Double valor13Salario) { this.valor13Salario = valor13Salario; }

    public Double getIrrfPago() { return irrfPago; }
    public void setIrrfPago(Double irrfPago) { this.irrfPago = irrfPago; }

    public Double getInss() { return inss; }
    public void setInss(Double inss) { this.inss = inss; }

    public PessoaJuridica getFontePagadora() { return fontePagadora; }
    public void setFontePagadora(PessoaJuridica fontePagadora) { this.fontePagadora = fontePagadora; }

    public Integer getIdDeclaracao() { return idDeclaracao; }
    public void setIdDeclaracao(Integer idDeclaracao) { this.idDeclaracao = idDeclaracao; }
}