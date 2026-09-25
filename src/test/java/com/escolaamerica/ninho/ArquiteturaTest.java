package com.escolaamerica.ninho;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Fronteiras entre features (SPEC §3.4), no modo estrito decidido para o MVP:
 *
 * <ol>
 *   <li>{@code shared} não depende de nenhuma feature;</li>
 *   <li>nenhuma feature usa {@code domain}, {@code repository} ou {@code api} de outra — chamadas entre
 *       features passam pelo {@code service} público da feature dona.</li>
 * </ol>
 *
 * {@code config} só faz wiring e pode depender de qualquer feature. Quando uma feature precisar ler um
 * tipo de {@code domain} de outra (ex.: um enum), a exceção entra nomeada em {@link #EXCECOES}, com o motivo.
 */
class ArquiteturaTest {

    private static final String BASE = "com.escolaamerica.ninho";

    private static final List<String> FEATURES = List.of(
        "iam", "estrutura", "pessoa", "recrutamento", "triagem", "lgpd", "arquivo", "notificacao", "relatorio");

    private static final List<String> CAMADAS_PRIVADAS = List.of("domain", "repository", "api");

    /**
     * Exceções nomeadas: feature consumidora → pacotes de outra feature que ela pode usar
     * (ex.: {@code "triagem" → Set.of(BASE + ".lgpd.domain..")}). Cada entrada precisa de um comentário com o motivo.
     */
    private static final Map<String, Set<String>> EXCECOES = Map.of();

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
    }

    @Test
    void sharedNaoDependeDeFeatures() {
        String[] pacotesDasFeatures = FEATURES.stream().map(f -> BASE + "." + f + "..").toArray(String[]::new);
        noClasses().that().resideInAPackage(BASE + ".shared..")
            .should().dependOnClassesThat().resideInAnyPackage(pacotesDasFeatures)
            .because("shared não depende de nenhuma feature (SPEC §3.4)")
            .check(classes);
    }

    @TestFactory
    List<DynamicTest> featureNaoUsaCamadasPrivadasDeOutra() {
        return FEATURES.stream()
            .flatMap(dona -> CAMADAS_PRIVADAS.stream().map(camada -> BASE + "." + dona + "." + camada + ".."))
            .map(privado -> DynamicTest.dynamicTest("só a feature dona usa " + privado, () -> {
                String dona = privado.substring(BASE.length() + 1, privado.indexOf('.', BASE.length() + 1));
                List<String> autorizados = new ArrayList<>(List.of(BASE + "." + dona + "..", BASE + ".config.."));
                EXCECOES.forEach((consumidora, liberados) -> {
                    if (liberados.contains(privado)) {
                        autorizados.add(BASE + "." + consumidora + "..");
                    }
                });
                noClasses().that().resideOutsideOfPackages(autorizados.toArray(String[]::new))
                    .should().dependOnClassesThat().resideInAPackage(privado)
                    .because("chamadas entre features passam pelo service público da feature dona (SPEC §3.4)")
                    .allowEmptyShould(true)
                    .check(classes);
            }))
            .toList();
    }
}
