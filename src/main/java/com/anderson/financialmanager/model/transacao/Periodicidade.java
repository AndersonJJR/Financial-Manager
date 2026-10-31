package com.anderson.financialmanager.model.transacao;

import java.time.LocalDate;

public enum Periodicidade {
    SEMANAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusWeeks(ciclos);
        }
    },
    QUINZENAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusDays(15L * ciclos);
        }
    },
    MENSAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusMonths(ciclos);
        }
    },
    BIMESTRAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusMonths(2L * ciclos);
        }
    },
    TRIMESTRAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusMonths(3L * ciclos);
        }
    },
    SEMESTRAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusMonths(6L * ciclos);
        }
    },
    ANUAL {
        @Override
        public LocalDate avancarData(LocalDate dataBase, int ciclos) {
            return dataBase.plusYears(ciclos);
        }
    };

    public abstract LocalDate avancarData(LocalDate dataBase, int ciclos);
}
