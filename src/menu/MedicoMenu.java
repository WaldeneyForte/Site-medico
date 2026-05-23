package menu;

import agendamento.AgendamentoMedico;
import agendamento.RealizarConsulta;
import login.medico;
import login.registro;
import login.usuario;
import wrapper.Printer;

import java.time.LocalDate;
import java.util.Map;

public class MedicoMenu extends MenuAfterLogin {
    @Override
    public void menuAfterLogin(usuario user) {
        if (!(user instanceof medico medico)) {
            Printer.println("Erro: Usuário não é um médico!");
            return;
        }

        RealizarConsulta realizarConsulta = new RealizarConsulta();
        AgendamentoMedico agendamentoMedico = new AgendamentoMedico();

        while (true) {
            Printer.println("Bom dia, Doutor(a) " + user.getNome());
            Printer.println("[1] Realizar Consulta");
            Printer.println("[2] Definir Horários Padrão");
            Printer.println("[3] Aplicar Horários Padrão a um Dia");
            Printer.println("[4] Alterar Dados da Conta");
            Printer.println("[5] Sair");

            int opcao;
            try {
                opcao = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                Printer.println("Opção inválida! Digite um número.");
                continue;
            }

            switch (opcao) {
                case 1:
                    realizarConsulta.realizarConsulta(medico);
                    break;
                case 2:
                    agendamentoMedico.definirHorariosPadrao(medico);
                    break;
                case 3:
                    Printer.print("Digite a data para aplicar os horários padrão (formato YYYY-MM-DD): ");
                    try {
                        LocalDate data = LocalDate.parse(scanner.nextLine());
                        agendamentoMedico.aplicarHorariosPadrao(medico, data);
                    } catch (Exception e) {
                        Printer.println("Data inválida! Use o formato YYYY-MM-DD.");
                    }
                    break;
                case 4:
                    alterarDadosMedico(medico);
                    break;
                case 5:
                    Printer.println("Saindo...");
                    return;
                default:
                    Printer.println("Opção inválida!");
                    break;
            }
        }
    }

    private void alterarDadosMedico(medico medico) {
        Printer.println("O que você deseja alterar?");
        Printer.println("[1] Email");
        Printer.println("[2] Senha");
        Printer.println("[3] Planos de Saúde Aceitos");
        Printer.println("[4] Voltar");

        int escolha;
        try {
            escolha = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            Printer.println("Opção inválida! Digite um número.");
            return;
        }

        switch (escolha) {
            case 1:
                Printer.print("Digite o novo email: ");
                String novoEmail = scanner.nextLine();
                Map<String, usuario> usuarios = registro.carregarUsuarios();
                if (!medico.getEmail().equals(novoEmail) && usuarios.containsKey(novoEmail)) {
                    Printer.println("Email já está em uso!");
                } else {
                    String emailAntigo = medico.getEmail();
                    medico.alterarEmail(novoEmail);
                    usuarios.remove(emailAntigo);
                    usuarios.put(novoEmail, medico);
                    registro.salvarUsuarios(usuarios);
                    Printer.println("Email alterado com sucesso!");
                }
                break;
            case 2:
                System.out.println("Digite a senha antiga: ");
                String senhaAntiga = scanner.nextLine();
                Printer.print("Digite a nova senha: ");
                String novaSenha = scanner.nextLine();
                try {
                    medico.alterarSenha(senhaAntiga, novaSenha);
                    Printer.println("Senha alterada com sucesso!");
                } catch (IllegalArgumentException e) {
                    Printer.println(e.getMessage());
                }
                break;
            case 3:
                medico.alterarPlano(null, scanner); // Passa o scanner existente
                break;
            case 4:
                Printer.println("Voltando ao menu...");
                break;
            default:
                Printer.println("Opção inválida!");
                break;
        }
    }
}
