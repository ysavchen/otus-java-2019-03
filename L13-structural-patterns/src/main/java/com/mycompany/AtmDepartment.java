package com.mycompany;

import com.mycompany.atm.ATM;

/**
 * Департамент ATM
 * Написать приложение ATM Департамент:
 * 1) Департамент может содержать несколько ATM.
 * 2) Департамент может собирать сумму остатков со всех ATM.
 * 3) Департамент может инициировать событие – восстановить состояние всех
 * ATM до начального (начальные состояния у разных ATM могут быть
 * разными).
 * Это тренировочное задание на применение паттернов.
 * Попробуйте использовать как можно больше.
 */
public interface AtmDepartment {

    void addATM(ATM atm);

    void removeATM(ATM atm);

    void restoreInitialStates();

    long getRemainders();

}
