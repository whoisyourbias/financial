package dev.whoisyourbias.financial.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {}
