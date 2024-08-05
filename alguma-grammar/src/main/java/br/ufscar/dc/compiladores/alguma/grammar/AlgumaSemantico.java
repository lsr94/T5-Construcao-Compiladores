package br.ufscar.dc.compiladores.alguma.grammar;

import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.IdentificadorContext;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaGrammarParser.Parcela_unarioContext;
import br.ufscar.dc.compiladores.alguma.grammar.Escopos;
import br.ufscar.dc.compiladores.alguma.grammar.AlgumaSemanticoUtils;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.AlgumaGrammar;
import br.ufscar.dc.compiladores.alguma.grammar.TabelaDeSimbolos.TipoEntrada;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.Token;

public class AlgumaSemantico extends AlgumaGrammarBaseVisitor<Void> {
    TabelaDeSimbolos tabela;
    TabelaDeSimbolos tabelaEscopos;

    static Escopos escoposAninhados = new Escopos();

    // Criação de uma tabela que armazenará as variáveis referentes a um registro
    HashMap<String, ArrayList<String>> tabelaRegistro = new HashMap<>();

    // Criação de uma tabela que armazenará os nomes e parâmetros das funções e procedimentos
    static HashMap<String, ArrayList<AlgumaGrammar>> tabelaFuncaoProcedimento = new HashMap<>();

    @Override
    public Void visitPrograma(AlgumaGrammarParser.ProgramaContext ctx) {
        // Inicialização do programa.
        tabela = new TabelaDeSimbolos();
        return super.visitPrograma(ctx);
    }

    // Adiciona variável e seu tipo na tabela de símbolos
    public void adicionaVariavelTabela(String nomeVariavel, String tipoVariavel, Token nomeToken, Token TipoToken, TipoEntrada tipoEnt) {
        tabelaEscopos = escoposAninhados.obterEscopoAtual();

        AlgumaGrammar tipoItem;
        boolean flagPonteiro = false;

        // Checa se o tipo da variável inicia com ^, identificando um ponteiro
        if (tipoVariavel.startsWith("^")) {
            // Caso sim, define a flag de ponteiro como verdadeira e remove o caractere ^ do tipo
            flagPonteiro = true;
            tipoVariavel = tipoVariavel.substring(1); 
        }

        switch (tipoVariavel) {
            case "literal":
                tipoItem = AlgumaGrammar.LITERAL;
                break;
            case "inteiro":
                tipoItem = AlgumaGrammar.INTEIRO;
                break;
            case "real":
                tipoItem = AlgumaGrammar.REAL;
                break;
            case "logico":
                tipoItem = AlgumaGrammar.LOGICO;
                break;
            default:
                tipoItem = AlgumaGrammar.INVALIDO;
                break;
        }

        // Verificando se o tipo da variável é 'INVÁLIDO' para retornar mensagem de erro
        if (tipoItem == AlgumaGrammar.INVALIDO) {
            tabelaEscopos.adicionar(nomeVariavel, tipoItem, tipoEnt, flagPonteiro);
            AlgumaSemanticoUtils.adicionarErroSemantico(TipoToken, "tipo " + tipoVariavel + " nao declarado");
        }

        /*
         * Se o tipo da variável não for 'INVÁLIDO', verifica se ela já existe
         * Caso não exista: adiciona
         * Caso exista: retorna erro semântico, pois já foi declarada
         */
        else if (!tabelaEscopos.existe(nomeVariavel))
            tabelaEscopos.adicionar(nomeVariavel, tipoItem, tipoEnt, flagPonteiro);
        else
            AlgumaSemanticoUtils.adicionarErroSemantico(nomeToken, "identificador " + nomeVariavel + " ja declarado anteriormente");

    }

    // Visita as declarações
    @Override
    public Void visitDeclaracoes(AlgumaGrammarParser.DeclaracoesContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.Decl_local_globalContext declaracao : ctx.decl_local_global())
            visitDecl_local_global(declaracao);

        return super.visitDeclaracoes(ctx);
    }

    // Visita as declarações locais
    @Override
    public Void visitDeclaracao_local(AlgumaGrammarParser.Declaracao_localContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        String tipoVariavel;
        String nomeVariavel;

        if (ctx.getText().contains("declare")) {
            // Caso 1: a declaração é um registro
            if (ctx.variavel().tipo().registro() != null) {
                for (AlgumaGrammarParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                    adicionaVariavelTabela(ident.getText(), "registro", ident.getStart(), ctx.tipo().getStart(), TipoEntrada.VARIAVEL);    

                    for (AlgumaGrammarParser.VariavelContext vc : ctx.variavel().tipo().registro().variavel()) {
                        tipoVariavel = ident.getText();

                        for (AlgumaGrammarParser.IdentificadorContext identc : vc.identificador())
                            adicionaVariavelTabela(ident.getText() + "." + identc.getText(), tipoVariavel, identc.getStart(), vc.tipo().getStart(), TipoEntrada.VARIAVEL);
                    }
                }
            } else { // Caso 2: o tipo já foi declarado
                tipoVariavel = ctx.variavel().tipo().getText();
                // Caso 2.1: verifica se é um registro já declarado
                if (tabelaRegistro.containsKey(tipoVariavel)) {
                    ArrayList<String> variavelRegistro = tabelaRegistro.get(tipoVariavel);

                    for (AlgumaGrammarParser.IdentificadorContext ic: ctx.variavel().identificador()) {
                        nomeVariavel = ic.IDENT().get(0).getText();

                        if (tabela.existe(nomeVariavel) || tabelaRegistro.containsKey(nomeVariavel)) 
                            AlgumaSemanticoUtils.adicionarErroSemantico(ic.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                            else {
                            adicionaVariavelTabela(nomeVariavel, "registro", ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);

                            for (int i = 0; i < variavelRegistro.size(); i = i + 2)
                                adicionaVariavelTabela(nomeVariavel + "." + variavelRegistro.get(i), variavelRegistro.get(i+1), ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);
                        }
                    }
                } else { // Caso 2.2: verifica se é um tipo básico já declarado
                    for (AlgumaGrammarParser.IdentificadorContext identc : ctx.variavel().identificador()) {
                        nomeVariavel = identc.getText();

                        // Verifica se pode ser uma função ou procedimento
                        if (tabelaFuncaoProcedimento.containsKey(nomeVariavel))
                            AlgumaSemanticoUtils.adicionarErroSemantico(identc.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                        else
                            adicionaVariavelTabela(nomeVariavel, tipoVariavel, identc.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL); 
                    }
                }
            }
        } else if (ctx.getText().contains("tipo")) {
            
            if (ctx.tipo().registro() != null) {
                ArrayList<String> variaveisRegistro = new ArrayList<>();
                
                for (AlgumaGrammarParser.VariavelContext vc : ctx.tipo().registro().variavel()) {
                    tipoVariavel = vc.tipo().getText();
                    
                    for (AlgumaGrammarParser.IdentificadorContext ic : vc.identificador()) {
                        variaveisRegistro.add(ic.getText());
                        variaveisRegistro.add(tipoVariavel);
                    }
                }
                tabelaRegistro.put(ctx.IDENT().getText(), variaveisRegistro);
            }
        // Verifica se é a declaração de uma constante.
        } else if (ctx.getText().contains("constante"))
            adicionaVariavelTabela(ctx.IDENT().getText(), ctx.tipo_basico().getText(), ctx.IDENT().getSymbol(), ctx.IDENT().getSymbol(), TipoEntrada.VARIAVEL);
        

        return super.visitDeclaracao_local(ctx);
    }

    @Override
    public Void visitDecl_local_global(AlgumaGrammarParser.Decl_local_globalContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        // Identifica se é uma declaração local ou global
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        } else if (ctx.declaracao_global() != null)
            visitDeclaracao_global(ctx.declaracao_global());

        return super.visitDecl_local_global(ctx);
    }

    // Visita atribuições
    @Override
    public Void visitCmdAtribuicao(AlgumaGrammarParser.CmdAtribuicaoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        String nomeVariavel = ctx.identificador().getText();
        //System.out.println("Visitando atribuição! Variável+" + ctx.identificador());
        boolean deferenciacao = ctx.start.getText().equals("^");
        boolean ref_memoria = ctx.expressao().getText().startsWith("&");
        // System.out.println("Inicio da expressao: "+ctx.start.getText()+".// ^?"+isDereferenced);
        AlgumaGrammar tipoExpressao = AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());

        if (tipoExpressao != AlgumaGrammar.INVALIDO) {
            // Caso a variável não tenha sido declarada, informa o erro
            if (!tabela.existe(nomeVariavel)) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(),
                        "identificador " + ctx.identificador().getText() + " nao declarado");
            } else {
                // Caso tenha sido declarada, verifica os demais casos
                AlgumaGrammar tipoVar = tabela.verificar(nomeVariavel);
                boolean isPointerVar = tabela.verificar_ponteiro(nomeVariavel);
                //System.out.println("Nome variavel: " + nomeVariavel + ". Tipo variavel:" + tipoVar + ". Pointer?" + isPointerVar);
                //System.out.println("Expressao: " + ctx.expressao() + " . Texto expressao: " + ctx.expressao().getText() + ". Tipo Expressao:" + tipoExpressao);

                // Se a variável é um ponteiro há 2 casos:
                if (isPointerVar){
                    /* Caso 1: Há deferenciação. Ex: distancia é ponteiro de int. ^distancia <- 400
                        * Testar se o tipo da variável é o mesmo da expressão.
                        * Testar se a variável é ponteiro.
                        * Erro? incompatível ^ */
                    if (deferenciacao && !ref_memoria) {
                        if (tipoVar != tipoExpressao)
                            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(),
                                    "atribuicao nao compativel para " + ctx.start.getText() + nomeVariavel);

                    }    
                    /* Caso 2: Atribuir o endereço da expressão à variável.
                    * Verificar se há o símbolo &
                    * Verificar se a expressão possui o mesmo tipo da variável
                    * Verificaar se a variável é ponteiro */
                    else if (ref_memoria && !deferenciacao){
                        String identificadorExpressao = extractIndentificadorString(ctx.expressao().getText());
                        // Caso a variável seja um ponteiro e o identificador da expressão não seja null
                        if (identificadorExpressao != null) {
                            // Verifica se o identificador da expressão existe na tabela de símbolos e são
                            // de mesmo tipo
                            AlgumaGrammar tipoId = tabela.verificar(identificadorExpressao);
                            if (tipoId != tipoVar) {
                                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(),
                                        "atribuicao nao compativel para " + nomeVariavel);
                            }
                        }
                    }
                    // Exceções: Erro.
                    else{
                        AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(),
                                        "atribuicao nao compativel para " + nomeVariavel);
                    }

                }
            
                // Caso a variável não seja ponteiro, verifica apenas o tipo
                else if (tipoExpressao != tipoVar)
                        AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(),
                                "tipo de atribuicao incompativel para " + nomeVariavel);
            }
        }
        return super.visitCmdAtribuicao(ctx);
    }

    private String extractIndentificadorString(String expression) {
        // Extrai o identificador após o símbolo '&''
        if (!expression.startsWith("&"))
            return null;

        return expression.substring(1).trim().split("\\s+")[0];
    }

    // Visita comandos de leitura
    @Override
    public Void visitCmdLeia(AlgumaGrammarParser.CmdLeiaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        for (AlgumaGrammarParser.IdentificadorContext id : ctx.identificador())
            if (!tabela.existe(id.getText()))
                AlgumaSemanticoUtils.adicionarErroSemantico(id.getStart(),
                        "identificador " + id.getText() + " nao declarado");

        return super.visitCmdLeia(ctx);
    }

    // Visita parcelas unárias
    @Override
    public Void visitParcela_unario(Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            IdentificadorContext idc = ctx.identificador();
            for (var id : idc.IDENT()) {
                String nomeVar = id.getText();
                //System.out.println("Parcela Unaria. Nome var: " + nomeVar);
                boolean declarado = false;
                for (var tabela : escoposAninhados.percorrerEscoposAninhados()) {
                    if (tabela.existe(nomeVar)) {
                        declarado = true;
                    }
                }
                if (!declarado) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(id.getSymbol(),
                            "identificador " + nomeVar + " nao declarado");
                }
            }
        }

        return super.visitParcela_unario(ctx);
    }

    @Override
    public Void visitCmdEnquanto(AlgumaGrammarParser.CmdEnquantoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();

        AlgumaSemanticoUtils.verificarTipo(tabela, ctx.expressao());

        return super.visitCmdEnquanto(ctx);
    }
}