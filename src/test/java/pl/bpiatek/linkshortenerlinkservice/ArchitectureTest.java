package pl.bpiatek.linkshortenerlinkservice;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("pl.bpiatek.linkshortenerlinkservice");

    @Test
    void linkModule_shouldBeCorrectlyEncapsulated() {
        ArchRule rule = classes()
                .that().resideInAPackage("..link..")
                .and().doNotHaveFullyQualifiedName("pl.bpiatek.linkshortenerlinkservice.link.LinkFacade")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAPackage("..link..");

        rule.check(importedClasses);
    }
}
