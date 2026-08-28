package dev.whoisyourbias.financial;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("architecture")
class ModuleArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.whoisyourbias.financial");
  }

  @Test
  void sharedKernelDoesNotDependOnLedgerOrBootstrap() {
    noClasses()
        .that()
        .resideInAPackage("..shared..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..ledger..", "..bootstrap..")
        .check(productionClasses);
  }

  @Test
  void ledgerDoesNotDependOnBootstrap() {
    noClasses()
        .that()
        .resideInAPackage("..ledger..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..bootstrap..")
        .check(productionClasses);
  }

  @Test
  void modulesOutsideLedgerDoNotAccessPersistenceTypes() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..ledger..")
        .should()
        .dependOnClassesThat()
        .haveSimpleNameEndingWith("Entity")
        .orShould()
        .dependOnClassesThat()
        .haveSimpleNameEndingWith("Repository")
        .check(productionClasses);
  }
}
