/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.exception;

/**
 *
 * @author Piotr
 */
public class DatabaseException extends Exception{    
    public transient final static String ERROR_USER_EXIST = "This user already exist in database";
    public transient final static String ERROR_USER_NOT_EXIST = "This user does not exist it database";
}
