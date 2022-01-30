import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
 
public class Lex {
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;
 
    Map<String, TokenType> keywords = new HashMap<>();
 
    static class Token {
        public TokenType tipotoken;
        public String valor;
        public int line;
        public int pos;
        Token(TokenType token, String valor, int linha, int pos) {
            this.tipotoken = token; this.valor = valor; this.line = linha; this.pos = pos;
        }
        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.line, this.pos, this.tipotoken);
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
 
    static enum TokenType {
        Finaldaentrada,    Op_multiplicacao,    Op_divisao,     Op_mod,        Op_soma,     Op_subtracao,
        Op_not,              Op_menor,      Op_menorigual, Op_maior,    Op_maiorigual,
        Op_igual,          Op_naoigual,         Op_atribuicao, Op_and,        Op_or,       Palavrachave_if,
        Palavrachave_else, Palavrachave_while,  Abparentese,   Fcparentese,   Abchave,     Fcchave,
        Pontoevirgula,     Virgula,             Identificador, Inteiro,       String,      Tipodado,
        Palavrachave_for,  Palavrachave_return, Palavrachave_include
    }
 
    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s na linha %d, coluna %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }
 
    Lex (String source) {
        this.line = 1;
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
    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.Finaldaentrada) {
            error(line, pos, String.format("caractere nao reconhecido: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);
    }
    Token char_lit(int line, int pos) {
        char c = getNextChar(); // skip opening quote
        int n = (int)c;
        if (c == '\'') {
            error(line, pos, "constante de caractere vazia");
        } else if (c == '\\') {
            c = getNextChar();
            if (c == 'n') {
                n = 10;
            } else if (c == '\\') {
                n = '\\';
            } else {
                error(line, pos, String.format("sequencia de escape nao reconhecida \\%c", c));
            }
        }
        if (getNextChar() != '\'') {
            error(line, pos, "constante multi caracteres");
        }
        getNextChar();
        return new Token(TokenType.Inteiro, "" + n, line, pos);
    }
    Token string_lit(char start, int line, int pos) {
        String result = "";
        while (getNextChar() != start) {
            if (this.chr == '\u0000') {
                error(line, pos, "EOF enquanto lia string");
            }
            if (this.chr == '\n') {
                error(line, pos, "EOL enquanto lia string");
            }
            result += this.chr;
        }
        getNextChar();
        return new Token(TokenType.String, result, line, pos);
    }
    Token div_or_comment(int line, int pos) {
        if (getNextChar() != '*') {
            return new Token(TokenType.Op_divisao, "", line, pos);
        }
        getNextChar();
        while (true) { 
            if (this.chr == '\u0000') {
                error(line, pos, "EOF em comentario");
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
    Token identifier_or_integer(int line, int pos) {
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
            error(line, pos, String.format("caractere de identificador ou inteiro não reconhecido: (%d) %c", (int)this.chr, this.chr));
        }
 
        if (Character.isDigit(text.charAt(0))) {
            if (!is_number) {
                error(line, pos, String.format("número inválido: %s", text));
            }
            return new Token(TokenType.Inteiro, text, line, pos);
        }
 
        if (this.keywords.containsKey(text)) {
            return new Token(this.keywords.get(text), "", line, pos);
        }
        return new Token(TokenType.Identificador, text, line, pos);
    }
    Token getToken() {
        int line, pos;
        while (Character.isWhitespace(this.chr)) {
            getNextChar();
        }
        line = this.line;
        pos = this.pos;
 
        switch (this.chr) {
            case '\u0000': return new Token(TokenType.Finaldaentrada, "", this.line, this.pos);
            case '/': return div_or_comment(line, pos);
            case '\'': return char_lit(line, pos);
            case '<': return follow('=', TokenType.Op_menorigual, TokenType.Op_menor, line, pos);
            case '>': return follow('=', TokenType.Op_maiorigual, TokenType.Op_maior, line, pos);
            case '=': return follow('=', TokenType.Op_igual, TokenType.Op_atribuicao, line, pos);
            case '!': return follow('=', TokenType.Op_naoigual, TokenType.Op_not, line, pos);
            case '&': return follow('&', TokenType.Op_and, TokenType.Finaldaentrada, line, pos);
            case '|': return follow('|', TokenType.Op_or, TokenType.Finaldaentrada, line, pos);
            case '"': return string_lit(this.chr, line, pos);
            case '{': getNextChar(); return new Token(TokenType.Abchave, "", line, pos);
            case '}': getNextChar(); return new Token(TokenType.Fcchave, "", line, pos);
            case '(': getNextChar(); return new Token(TokenType.Abparentese, "", line, pos);
            case ')': getNextChar(); return new Token(TokenType.Fcparentese, "", line, pos);
            case '+': getNextChar(); return new Token(TokenType.Op_soma, "", line, pos);
            case '-': getNextChar(); return new Token(TokenType.Op_subtracao, "", line, pos);
            case '*': getNextChar(); return new Token(TokenType.Op_multiplicacao, "", line, pos);
            case '%': getNextChar(); return new Token(TokenType.Op_mod, "", line, pos);
            case ';': getNextChar(); return new Token(TokenType.Pontoevirgula, "", line, pos);
            case ',': getNextChar(); return new Token(TokenType.Virgula, "", line, pos);
            default: return identifier_or_integer(line, pos);
        }
    }
 
    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.line++;
            this.pos = 0;
        }
        return this.chr;
    }
 
    void printTokens() {
        Token t;
        while ((t = getToken()).tipotoken != TokenType.Finaldaentrada) {
            System.out.println(t);
        }
        System.out.println(t);
    }
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
        } else {
            error(-1, -1, "No args");
        }
    }
}