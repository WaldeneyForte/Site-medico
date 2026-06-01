# Site Médico - Backend

Backend para um sistema de gerenciamento hospitalar desenvolvido em **Java** como projeto da disciplina de **Programação Orientada a Objetos (POO)**.

## 📋 Descrição do Projeto

Este projeto implementa a camada backend de um site médico/hospitalar, demonstrando conceitos fundamentais de Programação Orientada a Objetos como:

- **Encapsulamento**
- **Herança**
- **Polimorfismo**
- **Abstração**
- **Modularização**

O sistema permite o gerenciamento de:
- Pacientes
- Médicos
- Consultas e agendamentos
- Diagnósticos e prescrições
- Histórico médico

## 🎯 Objetivos Educacionais

Este projeto foi desenvolvido para consolidar aprendizados em:
- Modelagem de classes e relacionamentos entre objetos
- Design de aplicações orientadas a objetos
- Utilização de estruturas de dados
- Princípios SOLID e padrões de design
- Persistência de dados

## 🛠️ Tecnologias

- **Linguagem**: Java
- **IDE Recomendada**: Eclipse, IntelliJ IDEA ou NetBeans
- **Versão Java**: Java 8 ou superior

## 📁 Estrutura do Projeto

```
src/
├── models/              # Classes de modelo (Paciente, Médico, Consulta, etc)
├── services/            # Lógica de negócio e serviços
├── repositories/        # Camada de dados
├── exceptions/          # Exceções personalizadas
└── utils/              # Classes utilitárias
```

## 🚀 Como Usar

### Pré-requisitos
- Java JDK 8 ou superior instalado
- Git

### Clonando o Repositório

```bash
git clone https://github.com/WaldeneyForte/Site-medico.git
cd Site-medico
```

### Compilando o Projeto

```bash
# Usando javac
javac -d bin src/**/*.java

# Ou importar em uma IDE e compilar
```

### Executando

```bash
# Através da IDE ou
java -cp bin Main
```

## 📚 Classes Principais

### Paciente
```java
class Paciente {
  - cpf: String
  - nome: String
  - dataNascimento: LocalDate
  - telefone: String
  - endereco: String
  - historicoMedico: List<String>
}
```

### Médico
```java
class Médico {
  - crm: String
  - nome: String
  - especialidade: String
  - telefone: String
  - agenda: List<Consulta>
}
```

### Consulta
```java
class Consulta {
  - id: int
  - paciente: Paciente
  - medico: Médico
  - dataHora: LocalDateTime
  - diagnostico: String
  - prescricao: String
}
```

## ✨ Funcionalidades

- ✅ Cadastro e gerenciamento de pacientes
- ✅ Cadastro e gerenciamento de médicos
- ✅ Agendamento de consultas
- ✅ Registro de diagnósticos e prescrições
- ✅ Busca e filtro de dados
- ✅ Validação de dados de entrada

## 📖 Exemplos de Uso

```java
// Criar um novo paciente
Paciente paciente = new Paciente("123.456.789-00", "João Silva");
paciente.setDataNascimento(LocalDate.of(1990, 5, 15));

// Criar um médico
Medico medico = new Medico("123456", "Dr. Carlos");
medico.setEspecialidade("Cardiologia");

// Agendar uma consulta
Consulta consulta = new Consulta(paciente, medico, LocalDateTime.now());
servico.agendarConsulta(consulta);
```

## 📝 Conceitos de POO Aplicados

### Encapsulamento
Todos os atributos são privados com getters e setters públicos.

### Herança
Possível estrutura com classe base `Pessoa` para `Paciente` e `Médico`.

### Polimorfismo
Métodos sobrescritos para validação e tratamento específico de cada entidade.

### Abstração
Interfaces e classes abstratas para definir contratos de serviços.

## 🤝 Contribuições

Como projeto acadêmico, sugestões de melhorias são bem-vindas! Sinta-se livre para:
- Reportar bugs
- Sugerir novas funcionalidades
- Melhorar a documentação

## 📄 Licença

Este projeto é fornecido como material educacional.

## 👤 Autor

**WaldeneyForte**

