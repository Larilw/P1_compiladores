import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
 
/*
Descricao: Classe que realiza a análise léxica de um arquivo
*/
public class Lex {
    private int linha;
    private int pos;
    private int position;
    private char chr;
    private String s;
 
    Map<String, TokenType> keywords = new HashMap<>();
 
    /*
    Descricao: Classe que representa um token 
    */
    static class Token {
        public TokenType tipotoken;
        public String valor;
        public int linha;
        public int pos;
        Token(TokenType token, String valor, int linha, int pos) {
            this.tipotoken = token; this.valor = valor; this.linha = linha; this.pos = pos;
        }

        /*
        Descricao: Formata a função toString para apresentar o token da maneira desejada de acordo com sua classe
        Entrada: Nenhuma
        Retorno: String formatada
        */
        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.linha, this.pos, this.tipotoken);
            switch (this.tipotoken) {
                case Inteiro:
                    result += String.format("  %4s", valor);
                    break;
                case Identificador:
                    result += String.format(" %s", valor);
                    break;
                case String:
                    result += String.format(" \"%s\"", valor);
                    break;
            }
            return result;
        }
    }
 
    /*
    Descricao: Enum que lista todas as classes de token aceitas
    */
    static enum TokenType {
        Finaldaentrada,    Op_multiplicacao,    Op_divisao,     Op_mod,        Op_soma,         Op_subtracao,
        Op_not,            Op_menor,            Op_menorigual,  Op_maior,      Op_maiorigual,   Op_igual,
        Op_naoigual,       Op_atribuicao,       Op_and,         Op_or,         Palavrachave_if, Palavrachave_include,
        Palavrachave_else, Palavrachave_while,  Abparentese,    Fcparentese,   Abchave,         Fcchave,
        Pontoevirgula,     Virgula,             Identificador,  Inteiro,       String,          Tipodado,
        Palavrachave_for,  Palavrachave_return
    }
 
    /*
    Descricao: Função que imprime em qual linha e coluna ocorreu um erro e qual erro foi
    Entrada: Linha e coluna do erro e mensagem de erro
    Retorno: Nenhum
    */
    static void error(int linha, int pos, String msg) {
        if (linha > 0 && pos > 0) {
            System.out.printf("%s na linha %d, coluna %d\n", msg, linha, pos);
        } else {
            System.out.println(msg);
        }
    }
 
    /*
    Descricao: Cria uma instância da classe Lex com as informações do arquivo dado e identifica tokens de palavras chave
    Entrada: String com todos os dados do arquivo
    Retorno: Nenhum
    */
    Lex (String source) {
        this.linha = 1;
        this.pos = 0;
        this.position = 0;
        this.s = source;
        this.chr = this.s.charAt(0);
        this.keywords.put("if", TokenType.Palavrachave_if);
        this.keywords.put("else", TokenType.Palavrachave_else);
        this.keywords.put("while", TokenType.Palavrachave_while);
        this.keywords.put("for", TokenType.Palavrachave_for);
        this.keywords.put("return", TokenType.Palavrachave_return);
        this.keywords.put("include", TokenType.Palavrachave_include);
        this.keywords.put("int", TokenType.Tipodado);
        this.keywords.put("float", TokenType.Tipodado);
        this.keywords.put("char", TokenType.Tipodado);
        this.keywords.put("float", TokenType.Tipodado);

    }

    /*
    Descricao: Função que realiza a comparação de um caracter com um caractere esperado, criando novo token se não houver erros
    Entrada: Caractere esperado, tipo de token caso o caractere seja o esperado, tipo de token caso o caractere não seja o esperado, linha e coluna do caractere
    Retorno: Retorna o token criado
    */
    Token compara_caractere(char expect, TokenType ifyes, TokenType ifno, int linha, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", linha, pos);
        }
        if (ifno == TokenType.Finaldaentrada) {
            error(linha, pos, String.format("ERRO: caractere nao reconhecido: (%d) '%c'", (int)this.chr, this.chr));
            getNextChar();
            return getToken();
        }
        return new Token(ifno, "", linha, pos);
    }

    /*
    Descricao: Função que realiza a leitura de um caractere e checa erros possíveis, criando novo token se não houver erros
    Entrada: Linha e coluna do caractere
    Retorno: Retorna o token criado
    */
    Token leitura_char(int linha, int pos) {
        char c = getNextChar(); // skip opening quote
        int n = (int)c;
        if (c == '\'') {
            error(linha, pos, "ERRO: constante de caractere vazia");
            getNextChar();
            return getToken();
        } else if (c == '\\') {
            c = getNextChar();
            if (c == 'n') {
                n = 10;
            } else if (c == '\\') {
                n = '\\';
            } else {
                error(linha, pos, String.format("ERRO: sequencia de escape nao reconhecida \\%c", c));
                getNextChar();
                return getToken();
            }
        }
        if (getNextChar() != '\'') {
            error(linha, pos, "ERRO: constante multi caracteres");
            getNextChar();
            return getToken();
        }
        getNextChar();
        return new Token(TokenType.Inteiro, "" + n, linha, pos);
    }

    /*
    Descricao: Função que realiza a leitura de uma string e checa erros possíveis, criando novo token se não houver erros
    Entrada: Caractere de começo da string, linha e coluna desse caractere
    Retorno: Retorna o token criado
    */
    Token leitura_string(char start, int linha, int pos) {
        String result = "";
        while (getNextChar() != start) {
            if (this.chr == '\u0000') {
                error(linha, pos, "ERRO: EOF enquanto lia string");
            }
            if (this.chr == '\n') {
                error(linha, pos, "ERRO: EOL enquanto lia string");
                getNextChar();
                return getToken();
            }
            result += this.chr;
        }
        getNextChar();
        return new Token(TokenType.String, result, linha, pos);
    }

    /*
    Descricao: Função que checa se o token é operador de divisão ou comentário e cria o token se for divisão
    Entrada: Linha e posição do caractere
    Retorno: Retorna o token criado se for divisão
    */
    Token div_ou_comment(int linha, int pos) {
        if (getNextChar() != '*') {
            return new Token(TokenType.Op_divisao, "", linha, pos);
        }
        getNextChar();
        while (true) { 
            if (this.chr == '\u0000') {
                error(linha, pos, "ERRO: EOF em comentario");
            } else if (this.chr == '*') {
                if (getNextChar() == '/') {
                    getNextChar();
                    return getToken();
                }
            } else {
                getNextChar();
            }
        }
    }

    /*
    Descricao: Função que checa se o token é inteiro ou identificador e cria o token com o tipo correto
    Entrada: Linha e posição do caractere
    Retorno: Retorna o token criado
    */
    Token identificador_ou_inteiro(int linha, int pos) {
        boolean is_number = true;
        String text = "";
 
        while (Character.isAlphabetic(this.chr) || Character.isDigit(this.chr) || this.chr == '_') {
            text += this.chr;
            if (!Character.isDigit(this.chr)) {
                is_number = false;
            }
            getNextChar();
        }
 
        if (text.equals("")) {
            error(linha, pos, String.format("ERRO: caractere nao reconhecido: (%d) %c", (int)this.chr, this.chr));
            getNextChar();
            return getToken();
        }
 
        if (Character.isDigit(text.charAt(0))) {
            if (!is_number) {
                error(linha, pos, String.format("ERRO: numero invalido: %s", text));
                getNextChar();
                return getToken();
            }
            return new Token(TokenType.Inteiro, text, linha, pos);
        }
 
        if (this.keywords.containsKey(text)) {
            return new Token(this.keywords.get(text), "", linha, pos);
        }
        return new Token(TokenType.Identificador, text, linha, pos);
    }

    /*
    Descricao: Função que cria o próximo token
    Entrada: Nenhuma
    Retorno: Retorna o token criado
    */
    Token getToken() {
        int linha, pos;
        while (Character.isWhitespace(this.chr)) {
            getNextChar();
        }
        linha = this.linha;
        pos = this.pos;
 
        switch (this.chr) {
            case '\u0000': return new Token(TokenType.Finaldaentrada, "", this.linha, this.pos);
            case '/': return div_ou_comment(linha, pos);
            case '\'': return leitura_char(linha, pos);
            case '<': return compara_caractere('=', TokenType.Op_menorigual, TokenType.Op_menor, linha, pos);
            case '>': return compara_caractere('=', TokenType.Op_maiorigual, TokenType.Op_maior, linha, pos);
            case '=': return compara_caractere('=', TokenType.Op_igual, TokenType.Op_atribuicao, linha, pos);
            case '!': return compara_caractere('=', TokenType.Op_naoigual, TokenType.Op_not, linha, pos);
            case '&': return compara_caractere('&', TokenType.Op_and, TokenType.Finaldaentrada, linha, pos);
            case '|': return compara_caractere('|', TokenType.Op_or, TokenType.Finaldaentrada, linha, pos);
            case '"': return leitura_string(this.chr, linha, pos);
            case '{': getNextChar(); return new Token(TokenType.Abchave, "", linha, pos);
            case '}': getNextChar(); return new Token(TokenType.Fcchave, "", linha, pos);
            case '(': getNextChar(); return new Token(TokenType.Abparentese, "", linha, pos);
            case ')': getNextChar(); return new Token(TokenType.Fcparentese, "", linha, pos);
            case '+': getNextChar(); return new Token(TokenType.Op_soma, "", linha, pos);
            case '-': getNextChar(); return new Token(TokenType.Op_subtracao, "", linha, pos);
            case '*': getNextChar(); return new Token(TokenType.Op_multiplicacao, "", linha, pos);
            case '%': getNextChar(); return new Token(TokenType.Op_mod, "", linha, pos);
            case ';': getNextChar(); return new Token(TokenType.Pontoevirgula, "", linha, pos);
            case ',': getNextChar(); return new Token(TokenType.Virgula, "", linha, pos);
            default: return identificador_ou_inteiro(linha, pos);
        }
    }
 
    /*
    Descricao: Função que avança uma posição no arquivo e lê o próximo caractere, se houver
    Entrada: Nenhuma
    Retorno: Informações do próximo caractere do arquivo lido ou informaçaõ de finalizar a leitura
    */
    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.linha++;
            this.pos = 0;
        }
        return this.chr;
    }
 
    /*
    Descricao: Função que imprime a posição do token, seu tipo e se ele for identificador, inteiro ou string imprime seu valor
    Entrada: Nenhuma
    Retorno: Nenhum
    */
    void printTokens() {
        Token t;
        while ((t = getToken()).tipotoken != TokenType.Finaldaentrada) {
            System.out.println(t);
        }
        System.out.println(t);
    }

    /*
    Descricao: Função que solicita o nome do arquivo e apresenta o resultado da análise léxica
    Entrada: Nenhuma
    Retorno: Nenhum
    */
    public static void main(String[] args) {
        String arquivo = null;
        System.out.println("Digite o nome do arquivo:");
        Scanner entrada = new Scanner(System.in);
        arquivo = entrada.nextLine() + ".txt";
        System.out.println("Resultado da analise lexica:");
        if (arquivo != null) {
            try {
                File f = new File(arquivo);
                Scanner s = new Scanner(f);
                String source = " ";
                while (s.hasNext()) {
                    source += s.nextLine() + "\n";
                }
                Lex l = new Lex(source);
                l.printTokens();
            } catch(FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            }
        } 
    }  
}