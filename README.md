# Finanzo - Gestão Financeira Pessoal

Aplicativo Android nativo para controle financeiro pessoal, 100% offline, desenvolvido com Kotlin, Jetpack Compose e Material Design 3.

---

## 1. Requisitos para Executar o Projeto

- **Android Studio**: Ladybug / Meerkat (2024.2+) ou superior com suporte a Kotlin 2.x
- **JDK**: Java 11 ou Java 17
- **Android SDK**:
  - `compileSdk`: 36
  - `targetSdk`: 36
  - `minSdk`: 24 (Android 7.0 Nougat ou superior)
- **Gradle**: Gerenciado pelo Gradle Wrapper / CLI do ambiente

---

## 2. Arquitetura e Decisões de Projeto

O aplicativo foi estruturado com foco em simplicidade, manutenibilidade e separação de responsabilidades (Clean Architecture / MVVM adaptado):

```
com.example
├── data
│   ├── dao           # DAOs do Room (TransactionDao, CategoryDao, BudgetDao)
│   ├── database      # AppDatabase, TypeConverters e dados predefinidos (PredefinedData)
│   ├── model         # Entidades de dados (TransactionEntity, CategoryEntity, BudgetEntity, TransactionType)
│   ├── preferences   # Repositório de preferências do usuário via Jetpack DataStore
│   └── repository    # FinanceRepository (abstração de acesso aos dados para UI)
├── domain
│   ├── calculator    # Regras puras de negócio e cálculos financeiros (FinanceCalculator)
│   ├── model         # Modelos de domínio (BudgetProgress, CategorySummary, MonthlyFinanceSummary, DateFilter)
│   └── util          # Utilitários de formatação de moeda (CurrencyUtils) e datas (DateUtils)
└── ui
    ├── budgets       # Tela de metas orçamentárias e diálogo de edição
    ├── categories    # Tela de categorias (predefinidas e personalizadas) e diálogo de edição
    ├── components    # Componentes reutilizáveis (CategoryIconBadge, CurrencyText, EmptyStateView, etc.)
    ├── dashboard     # Painel mensal, cards de saldo, resumo por categoria e lançamentos recentes
    ├── navigation    # Barra de navegação adaptativa (NavigationBar / NavigationRail para tablets)
    ├── settings      # Configurações de tema (Claro/Escuro/Sistema), privacidade e dados de demonstração
    ├── theme         # Tema Material 3 com paleta personalizada esmeralda/verde financeiro
    └── transactions  # Lista de lançamentos com filtros e diálogo de cadastro/edição
```

### Principais Decisões Técnicas

1. **Armazenamento Monetário com Precisão (Sem `Double`)**:
   - Valores monetários são armazenados estritamente em **centavos como `Long`** (`amountCents: Long`).
   - Evita problemas clássicos de arredondamento de ponto flutuante IEEE 754.
   - Cálculos e agregações em SQL (`SUM`) e Kotlin operam em inteiros exatos, e formatações usam `BigDecimal` e `NumberFormat` configurado para a localidade brasileira (`pt-BR`).
2. **Persistência Local Reativa com Room**:
   - Tabelas para lançamentos (`transactions`), categorias (`categories`) e orçamentos (`budgets`).
   - Consultas retornando `Flow<List<T>>` garantem que qualquer alteração em uma tela seja refletida instantaneamente em todo o app.
   - Operações de escrita em suspensão com `Dispatchers.IO`.
3. **Preferências Simples com DataStore**:
   - `UserPreferencesRepository` gerencia preferências do usuário (modo do tema: Sistema, Claro ou Escuro; alternância para ocultar valores na tela inicial).
4. **Interface Adaptativa e Acessível (Material 3)**:
   - Uso de `enableEdgeToEdge()`.
   - Layout responsivo: no celular exibe `NavigationBar` inferior; em telas maiores / tablets (>= 600dp) utiliza `NavigationRail` lateral.
   - Ícones e badges intuitivos para categorias.
   - Indicadores de acessibilidade e `testTag` em todos os fluxos principais.
5. **Dados de Demonstração (Seed)**:
   - Botão nas configurações permite carregar dados realistas (salário, freelance, rendimentos, despesas de alimentação, moradia, transporte, lazer e orçamentos) para experimentação instantânea da interface.

---

## 3. Funcionalidades Entregues

- [x] **Gestão de Lançamentos**:
  - Cadastro, edição e exclusão de receitas e despesas.
  - Campos: valor monetário exato, descrição, categoria, data e observação.
- [x] **Categorias Flexíveis**:
  - 13 categorias predefinidas de receitas e despesas com ícones e cores temáticas.
  - Criação, edição e exclusão de categorias personalizadas (com seleção de nome, cor e ícone).
- [x] **Saldo e Totais**:
  - Saldo atual e totais de receitas e despesas calculados com exatidão.
  - Opção de ocultar valores na interface (modo privacidade).
- [x] **Painel Mensal (Dashboard)**:
  - Navegação entre meses (anterior, atual, posterior).
  - Resumo de receitas, despesas e saldo do período selecionado.
  - Distribuição percentual e valor dos gastos por categoria com barras de progresso visuais.
  - Acesso rápido aos últimos lançamentos e criação ágil.
- [x] **Filtros Avançados**:
  - Filtro por tipo: Todas, Receitas, Despesas.
  - Filtro por período: Este Mês, Mês Anterior, Este Ano, Todos.
  - Filtro por categoria específica.
  - Busca textual em tempo real por descrição ou nota.
- [x] **Orçamento Mensal por Categoria**:
  - Metas mensais configuráveis por categoria de despesa.
  - Indicação visual do consumo: Verde (< 75%), Âmbar (75-99%), Vermelho (>= 100% ou ultrapassado).
  - Exibição de valor gasto, valor limite e saldo restante ou valor excedido.
- [x] **Persistência e Estado**:
  - Persistência local em Room Database.
  - Preferências salvas com Jetpack DataStore Preferences.
  - Estados vazios (*empty states*) ilustrados e mensagens de validação claras.
- [x] **Temas**:
  - Suporte completo a Tema Claro (*Light*), Tema Escuro (*Dark*) e Padrão do Sistema.

---

## 4. Como Compilar, Testar e Instalar

### Compilar o Aplicativo
```bash
gradle :app:assembleDebug
```

### Executar os Testes Automatizados
```bash
# Executa testes unitários e de persistência local (Robolectric)
gradle :app:testDebugUnitTest
```

### Instalar no Dispositivo ou Emulador
```bash
gradle :app:installDebug
```

---

## 5. Limitações Conhecidas

- **Sem Sincronização em Nuvem**: Como especificado para o MVP offline, os dados residem estritamente no dispositivo SQLite local.
- **Exportação/Importação CSV/PDF**: O backup atual ocorre pelo mecanismo padrão de backup do Android (`backup_rules.xml`). Exportação manual para planilhas pode ser adicionada em versões futuras.
