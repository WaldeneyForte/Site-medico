package agendamento;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import login.medico;
import login.usuario;
import wrapper.Printer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de agendamentos médicos com suporte a horários padrão.
 * Implementa thread-safety e caching para melhor desempenho.
 */
public class AgendamentoMedico {
    private static final String ARQUIVO_JSON = "horarios_medicos.json";
    private static final String ARQUIVO_PADRAO_JSON = "horarios_padrao_medicos.json";
    private static final int MAX_HORARIOS_POR_DIA = 4;
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger LOGGER = Logger.getLogger(AgendamentoMedico.class.getName());

    private final Map<String, Map<LocalDate, List<LocalTime>>> horariosDisponiveis;
    private final Map<String, List<LocalTime>> horariosPadrao;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Gson gson;

    public AgendamentoMedico() {
        this.horariosDisponiveis = new ConcurrentHashMap<>();
        this.horariosPadrao = new ConcurrentHashMap<>();
        this.gson = criarGson();
        carregarDados();
    }

    /**
     * Cria instância compartilhada do Gson com adapters customizados
     */
    private Gson criarGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
            .setPrettyPrinting()
            .create();
    }

    /**
     * Carrega dados de horários e horários padrão de forma sincronizada
     */
    private void carregarDados() {
        lock.writeLock().lock();
        try {
            horariosDisponiveis.putAll(carregarHorarios());
            horariosPadrao.putAll(carregarHorariosPadrao());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Valida se o usuário é médico e registra erro se não for
     */
    private boolean validarMedico(usuario usuario, String operacao) {
        if (!(usuario instanceof medico)) {
            Printer.println("Apenas médicos podem " + operacao + "!");
            LOGGER.log(Level.WARNING, "Tentativa de " + operacao + " por não-médico");
            return false;
        }
        return true;
    }

    /**
     * Obtém ou cria mapa de horários do médico
     */
    private Map<LocalDate, List<LocalTime>> obterHorariosDoMedico(String emailMedico, boolean criar) {
        Map<LocalDate, List<LocalTime>> horarios = horariosDisponiveis.get(emailMedico);
        if (horarios == null && criar) {
            horarios = new ConcurrentHashMap<>();
            horariosDisponiveis.put(emailMedico, horarios);
        }
        return horarios != null ? horarios : new ConcurrentHashMap<>();
    }

    /**
     * Obtém ou cria lista de horários para um dia específico
     */
    private List<LocalTime> obterHorariosDoDia(Map<LocalDate, List<LocalTime>> horariosDoMedico, 
                                               LocalDate data, boolean criar) {
        List<LocalTime> horarios = horariosDoMedico.get(data);
        if (horarios == null && criar) {
            horarios = Collections.synchronizedList(new ArrayList<>());
            horariosDoMedico.put(data, horarios);
        }
        return horarios != null ? horarios : new ArrayList<>();
    }

    /**
     * Adiciona horário disponível para um médico em uma data específica
     */
    public void adicionarHorarioDisponivel(usuario medico, LocalDate data, LocalTime horario) {
        if (!validarMedico(medico, "alterar horários")) {
            return;
        }

        if (data == null || horario == null) {
            Printer.println("Data e horário não podem ser nulos!");
            return;
        }

        lock.writeLock().lock();
        try {
            String emailMedico = medico.getEmail();
            Map<LocalDate, List<LocalTime>> horariosDoMedico = obterHorariosDoMedico(emailMedico, true);
            List<LocalTime> horariosDoDia = obterHorariosDoDia(horariosDoMedico, data, true);

            if (horariosDoDia.size() >= MAX_HORARIOS_POR_DIA) {
                Printer.println("Limite máximo de " + MAX_HORARIOS_POR_DIA + " horários por dia atingido!");
                return;
            }

            if (horariosDoDia.contains(horario)) {
                Printer.println("Este horário já está registrado para o dia " + data + "!");
                return;
            }

            horariosDoDia.add(horario);
            salvarHorarios();
            Printer.println("Horário " + horario + " adicionado com sucesso para o dia " + data + "!");
            LOGGER.log(Level.INFO, "Horário adicionado: " + emailMedico + " - " + data + " " + horario);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove horário disponível para um médico em uma data específica
     */
    public void removerHorarioDisponivel(usuario medico, LocalDate data, LocalTime horario) {
        if (!validarMedico(medico, "alterar horários")) {
            return;
        }

        if (data == null || horario == null) {
            Printer.println("Data e horário não podem ser nulos!");
            return;
        }

        lock.writeLock().lock();
        try {
            String emailMedico = medico.getEmail();
            Map<LocalDate, List<LocalTime>> horariosDoMedico = horariosDisponiveis.get(emailMedico);

            if (horariosDoMedico == null || !horariosDoMedico.containsKey(data)) {
                Printer.println("Nenhum horário disponível encontrado para o dia " + data + "!");
                return;
            }

            List<LocalTime> horariosDoDia = horariosDoMedico.get(data);
            if (horariosDoDia.remove(horario)) {
                if (horariosDoDia.isEmpty()) {
                    horariosDoMedico.remove(data);
                }
                salvarHorarios();
                Printer.println("Horário " + horario + " removido com sucesso do dia " + data + "!");
                LOGGER.log(Level.INFO, "Horário removido: " + emailMedico + " - " + data + " " + horario);
            } else {
                Printer.println("Horário " + horario + " não encontrado para o dia " + data + "!");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Define horários padrão para um médico
     */
    public void definirHorariosPadrao(usuario medico) {
        if (!validarMedico(medico, "definir horários padrão")) {
            return;
        }

        String emailMedico = medico.getEmail();
        List<LocalTime> horariosPadraoMedico = new ArrayList<>();

        Printer.println("Defina até " + MAX_HORARIOS_POR_DIA + " horários padrão (formato HH:MM). Digite 'fim' para encerrar.");
        
        while (horariosPadraoMedico.size() < MAX_HORARIOS_POR_DIA) {
            Printer.print("Horário " + (horariosPadraoMedico.size() + 1) + ": ");
            String entrada = scanner.nextLine().trim();
            
            if (entrada.equalsIgnoreCase("fim")) {
                break;
            }

            try {
                LocalTime horario = LocalTime.parse(entrada);
                if (horariosPadraoMedico.contains(horario)) {
                    Printer.println("Horário já adicionado! Tente outro.");
                } else {
                    horariosPadraoMedico.add(horario);
                    Printer.println("Horário " + horario + " adicionado ao padrão.");
                }
            } catch (Exception e) {
                Printer.println("Horário inválido! Use o formato HH:MM (ex.: 09:00).");
                LOGGER.log(Level.WARNING, "Formato de horário inválido: " + entrada);
            }
        }

        if (horariosPadraoMedico.isEmpty()) {
            Printer.println("Nenhum horário padrão definido.");
            return;
        }

        lock.writeLock().lock();
        try {
            horariosPadrao.put(emailMedico, Collections.unmodifiableList(horariosPadraoMedico));
            salvarHorariosPadrao();
            Printer.println("Horários padrão salvos com sucesso!");
            LOGGER.log(Level.INFO, "Horários padrão definidos para: " + emailMedico);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Aplica horários padrão a um dia específico
     */
    public void aplicarHorariosPadrao(usuario medico, LocalDate data) {
        if (!validarMedico(medico, "aplicar horários padrão")) {
            return;
        }

        if (data == null) {
            Printer.println("Data não pode ser nula!");
            return;
        }

        lock.readLock().lock();
        List<LocalTime> padrao;
        try {
            String emailMedico = medico.getEmail();
            padrao = horariosPadrao.get(emailMedico);
            
            if (padrao == null || padrao.isEmpty()) {
                Printer.println("Nenhum horário padrão definido para este médico!");
                return;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            String emailMedico = medico.getEmail();
            Map<LocalDate, List<LocalTime>> horariosDoMedico = obterHorariosDoMedico(emailMedico, true);
            List<LocalTime> horariosDoDia = obterHorariosDoDia(horariosDoMedico, data, true);

            int adicionados = 0;
            for (LocalTime horario : padrao) {
                if (horariosDoDia.size() >= MAX_HORARIOS_POR_DIA) {
                    Printer.println("Limite de " + MAX_HORARIOS_POR_DIA + " horários atingido para o dia " + data + "!");
                    break;
                }
                if (!horariosDoDia.contains(horario)) {
                    horariosDoDia.add(horario);
                    adicionados++;
                }
            }

            if (adicionados > 0) {
                salvarHorarios();
                LOGGER.log(Level.INFO, "Horários padrão aplicados para: " + emailMedico + " - " + data);
            }
            Printer.println("Horários padrão aplicados ao dia " + data + " com sucesso!");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Obtém horários disponíveis para um médico em uma data específica
     */
    public List<LocalTime> getHorariosDisponiveis(usuario medico, LocalDate data) {
        if (data == null) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            String emailMedico = medico.getEmail();
            Map<LocalDate, List<LocalTime>> horariosDoMedico = horariosDisponiveis.getOrDefault(emailMedico, new ConcurrentHashMap<>());
            List<LocalTime> horarios = horariosDoMedico.getOrDefault(data, new ArrayList<>());
            return new ArrayList<>(horarios); // Retorna cópia para evitar modificações externas
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Carrega horários disponíveis do arquivo JSON
     */
    private Map<String, Map<LocalDate, List<LocalTime>>> carregarHorarios() {
        return carregarArquivoJSON(ARQUIVO_JSON, 
            new TypeToken<Map<String, Map<LocalDate, List<LocalTime>>>>() {}.getType());
    }

    /**
     * Carrega horários padrão do arquivo JSON
     */
    private Map<String, List<LocalTime>> carregarHorariosPadrao() {
        return carregarArquivoJSON(ARQUIVO_PADRAO_JSON,
            new TypeToken<Map<String, List<LocalTime>>>() {}.getType());
    }

    /**
     * Método genérico para carregar dados do arquivo JSON
     */
    @SuppressWarnings("unchecked")
    private <T> T carregarArquivoJSON(String nomeArquivo, Type tipo) {
        File arquivo = new File(nomeArquivo);
        
        if (!arquivo.exists() || arquivo.length() == 0) {
            try {
                if (!arquivo.exists()) {
                    arquivo.createNewFile();
                }
                try (FileWriter writer = new FileWriter(arquivo)) {
                    writer.write("{}");
                }
                LOGGER.log(Level.INFO, "Arquivo criado: " + nomeArquivo);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erro ao criar arquivo: " + nomeArquivo, e);
            }
            return (T) new ConcurrentHashMap<>();
        }

        try (FileReader reader = new FileReader(arquivo)) {
            T resultado = gson.fromJson(reader, tipo);
            return resultado != null ? resultado : (T) new ConcurrentHashMap<>();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar arquivo: " + nomeArquivo, e);
            return (T) new ConcurrentHashMap<>();
        }
    }

    /**
     * Salva horários disponíveis no arquivo JSON
     */
    private void salvarHorarios() {
        salvarArquivoJSON(ARQUIVO_JSON, horariosDisponiveis);
    }

    /**
     * Salva horários padrão no arquivo JSON
     */
    private void salvarHorariosPadrao() {
        salvarArquivoJSON(ARQUIVO_PADRAO_JSON, horariosPadrao);
    }

    /**
     * Método genérico para salvar dados em arquivo JSON
     */
    private void salvarArquivoJSON(String nomeArquivo, Object dados) {
        File arquivo = new File(nomeArquivo);
        try (FileWriter writer = new FileWriter(arquivo)) {
            gson.toJson(dados, writer);
            LOGGER.log(Level.INFO, "Dados salvos em: " + nomeArquivo);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar arquivo: " + nomeArquivo, e);
            throw new RuntimeException("Falha ao salvar os dados em: " + nomeArquivo, e);
        }
    }
}
