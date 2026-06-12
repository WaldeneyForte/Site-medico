package login;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import wrapper.Printer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador centralizado de autenticação e sessão de usuários.
 * Implementa padrão Singleton com caching para otimizar acesso aos dados de usuários.
 * 
 * @author Site Médico
 * @version 2.0
 */
public class Login {
    private static final Logger logger = Logger.getLogger(Login.class.getName());
    private static final String ARQUIVO_JSON = "usuarios.json";
    private static final Login INSTANCE = new Login();
    
    private static usuario currentUser;
    private static Map<String, usuario> usuariosCache;
    private static long ultimaCarregamento = 0;
    private static final long CACHE_TIMEOUT = 300000; // 5 minutos em ms

    private Login() {
        // Inicializa cache vazio
        usuariosCache = new HashMap<>();
    }

    /**
     * Obtém instância única de Login (Singleton).
     * @return Instância de Login
     */
    public static Login getInstance() {
        return INSTANCE;
    }

    /**
     * Obtém o usuário autenticado na sessão atual.
     * @return usuario autenticado ou null se nenhum usuário está logado
     */
    public static usuario getCurrentUser() {
        return currentUser;
    }

    /**
     * Define o usuário autenticado da sessão.
     * @param user Usuário a ser definido como ativo
     */
    public static void setCurrentUser(usuario user) {
        Login.currentUser = user;
        if (user != null) {
            logger.info("Usuário logado com sucesso");
        }
    }

    /**
     * Faz logout do usuário atual.
     */
    public static void logout() {
        if (currentUser != null) {
            logger.info("Usuário desconectado");
            currentUser = null;
        }
    }

    /**
     * Autentica um usuário com email e senha.
     * 
     * @param email Email do usuário
     * @param senha Senha em texto plano
     * @return usuario autenticado ou null se falhar
     */
    public static usuario autenticar(String email, String senha) {
        try {
            // Validações de entrada
            if (email == null || email.trim().isEmpty()) {
                logger.warning("Tentativa de login com email vazio");
                Printer.println("Email não pode estar vazio.");
                return null;
            }

            if (senha == null || senha.trim().isEmpty()) {
                logger.warning("Tentativa de login com senha vazia");
                Printer.println("Senha não pode estar vazia.");
                return null;
            }

            // Normaliza email para minúsculas
            email = email.trim().toLowerCase();

            Map<String, usuario> usuarios = carregarUsuarios();
            
            if (!usuarios.containsKey(email)) {
                logger.warning("Email não encontrado: " + email);
                Printer.println("Email não encontrado no sistema.");
                return null;
            }

            usuario user = usuarios.get(email);
            
            if (!user.verificarSenha(senha)) {
                logger.warning("Senha incorreta para email: " + email);
                Printer.println("Senha incorreta.");
                return null;
            }

            currentUser = user;
            logger.info("Login bem-sucedido para: " + email);
            Printer.println("Login bem-sucedido!");
            return user;

        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Arquivo de usuários não encontrado", e);
            Printer.println("Arquivo de dados não encontrado.");
            return null;
        } catch (JsonSyntaxException e) {
            logger.log(Level.SEVERE, "Erro ao parsear JSON de usuários", e);
            Printer.println("Erro ao carregar dados de usuários.");
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro inesperado durante autenticação", e);
            Printer.println("Erro ao tentar autenticar: " + e.getMessage());
            return null;
        }
    }

    /**
     * Carrega usuários do arquivo JSON com caching.
     * Cache é invalidado após 5 minutos ou quando explicitamente recarregado.
     * 
     * @return Mapa de usuários (email -> usuario)
     * @throws IOException se houver erro ao ler o arquivo
     */
    private static Map<String, usuario> carregarUsuarios() throws IOException {
        long tempoAtual = System.currentTimeMillis();

        // Retorna cache se ainda é válido
        if (!usuariosCache.isEmpty() && (tempoAtual - ultimaCarregamento) < CACHE_TIMEOUT) {
            logger.fine("Usuários carregados do cache");
            return usuariosCache;
        }

        // Recarrega do arquivo JSON
        try (FileReader reader = new FileReader(ARQUIVO_JSON)) {
            logger.info("Carregando usuários do arquivo JSON");

            RuntimeTypeAdapterFactory<usuario> adapter = RuntimeTypeAdapterFactory
                .of(usuario.class, "tipo")
                .registerSubtype(medico.class, "medico")
                .registerSubtype(paciente.class, "paciente");

            Type tipo = new TypeToken<Map<String, usuario>>() {}.getType();
            Map<String, usuario> usuarios = new GsonBuilder()
                .registerTypeAdapterFactory(adapter)
                .create()
                .fromJson(reader, tipo);

            // Normaliza todas as chaves para minúsculas e armazena no cache
            Map<String, usuario> usuariosNormalizados = new HashMap<>();
            if (usuarios != null) {
                usuarios.forEach((email, user) -> 
                    usuariosNormalizados.put(email.toLowerCase(), user)
                );
            }

            usuariosCache = usuariosNormalizados;
            ultimaCarregamento = tempoAtual;

            logger.info("Usuários carregados com sucesso. Total: " + usuariosCache.size());
            return usuariosCache;

        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Arquivo JSON não encontrado: " + ARQUIVO_JSON, e);
            usuariosCache = new HashMap<>();
            throw e;
        } catch (JsonSyntaxException e) {
            logger.log(Level.SEVERE, "Erro ao parsear JSON: sintaxe inválida", e);
            usuariosCache = new HashMap<>();
            throw e;
        }
    }

    /**
     * Limpa o cache de usuários de forma forçada.
     * Útil para recarregar dados após atualizações no arquivo.
     */
    public static void limparCache() {
        usuariosCache.clear();
        ultimaCarregamento = 0;
        logger.info("Cache de usuários foi limpo");
    }

    /**
     * Recarrega os usuários do arquivo JSON, ignorando o cache.
     * @return Mapa atualizado de usuários
     * @throws IOException se houver erro ao ler o arquivo
     */
    public static Map<String, usuario> recarregarUsuarios() throws IOException {
        limparCache();
        return carregarUsuarios();
    }
}
