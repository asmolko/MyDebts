package com.dao.mydebts;

import com.dao.mydebts.entities.StoredDebt;
import com.dao.mydebts.repos.StoredDebtRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Engine implementation based on simple DFS
 *
 * @author Oleg Chernovskiy
 */
@Controller
class SettlementEngineImpl implements SettlementEngine {

    @Autowired
    StoredDebtRepo sdRepo;

    @Override
    void relax(StoredDebt debt) {
        List<StoredDebt> allDebts = sdRepo.findAll();
    }

}
