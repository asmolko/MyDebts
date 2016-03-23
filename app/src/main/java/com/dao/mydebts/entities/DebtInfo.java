package com.dao.mydebts.entities;

import java.math.BigDecimal;

/**
 * Identifies debt from one person to another in specific group matrix.
 * 
 * Created by Oleg Chernovskiy on 23.03.16.
 */
public class DebtInfo {
    
    private GroupMatrix parent;
    
    private Person from;
    
    private Person to;
    
    private BigDecimal amount;
    
}
