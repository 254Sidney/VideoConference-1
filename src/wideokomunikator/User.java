/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator;

import java.io.Serializable;

/**
 *
 * @author Piotr
 */
public class User implements Serializable,Comparable<User>{
    protected int ID;
    protected String EMAIL;
    protected String FIRST_NAME;
    protected String LAST_NAME;

    public User() {
    }

    public User(int ID, String EMAIL, String FIRST_NAME, String LAST_NAME) {
        this.ID = ID;
        this.EMAIL = EMAIL;
        this.FIRST_NAME = FIRST_NAME;
        this.LAST_NAME = LAST_NAME;
    }

    public int getID() {
        return ID;
    }

    public String getEMAIL() {
        return EMAIL;
    }

    public String getFIRST_NAME() {
        return FIRST_NAME;
    }

    public String getLAST_NAME() {
        return LAST_NAME;
    }

    @Override
    public String toString() {
        return EMAIL+" "+FIRST_NAME+" "+LAST_NAME+" "+ID;
    }

    
    @Override
    public int compareTo(User o) {
        if(this.ID == o.ID){
            return 0;
        }
        int i = getLAST_NAME().compareTo(o.getLAST_NAME());
        if(i==0){
            i = getFIRST_NAME().compareTo(o.getLAST_NAME());
            if(i==0){
                i = getEMAIL().compareTo(o.getEMAIL());
            }
        }
        return i;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof User) {
            User user = (User) obj;
            if (user.getID() == ID) {
                return true;
            }
        }
        return false;
    }
    
    
    
    
    
}
