package com.dao.mydebts.entities;

import java.util.List;

/**
 * Holds links between multiple persons and their debt plan
 * between each other.
 * 
 * Created by Oleg Chernovskiy on 23.03.16.
 */
public class GroupMatrix {
    
    private List<Person> participants;
    
    private List<DebtInfo> debts;
    
}
