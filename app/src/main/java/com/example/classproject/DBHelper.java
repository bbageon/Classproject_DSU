package com.example.classproject;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "order.db"; // DB 이름

    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // 테이블 생성 (기본키 : id (상품번호) / 속성1 : name (상품명) / 속성2 : quantity (수량) / 속성3 : sales (판매량) / 속성4 : stock (재고량) / 속성5 : date (날짜)
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory (id INTEGER PRIMARY KEY, name TEXT NOT NULL, quantity INTEGER NOT NULL, sales INTEGER, stock INTEGER NOT NULL, date TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    // SELECT문 - 특정 ID의 상품 정보 가져오기
    public InventoryItem getInventoryItemById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Inventory WHERE id=?", new String[]{String.valueOf(id)});
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
            int sales = cursor.getInt(cursor.getColumnIndexOrThrow("sales"));
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            InventoryItem inventoryItem = new InventoryItem();
            inventoryItem.setId(id);
            inventoryItem.setName(name);
            inventoryItem.setQuantity(quantity);
            inventoryItem.setSales(sales);
            inventoryItem.setStock(stock);
            inventoryItem.setDate(date);

            cursor.close();
            return inventoryItem;
        }
        cursor.close();
        return null;
    }

    // SELECT문 - 모두
    public ArrayList<InventoryItem> getInventoryItems() {
        ArrayList<InventoryItem> inventoryItems = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Inventory ORDER BY stock ASC", null); // 재고량 내림차순
        if(cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                int sales = cursor.getInt(cursor.getColumnIndexOrThrow("sales"));
                int stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                InventoryItem inventoryItem = new InventoryItem();
                inventoryItem.setId(id);
                inventoryItem.setName(name);
                inventoryItem.setQuantity(quantity);
                inventoryItem.setSales(sales);
                inventoryItem.setStock(stock);
                inventoryItem.setDate(date);
                inventoryItems.add(inventoryItem);
            }
        }
        cursor.close();

        return inventoryItems;
    }

    public void InsertInventory(int id, String name, int quantity) {
        SQLiteDatabase db = getWritableDatabase();

        // ID 중복 시 재확인 (팝업창)
        InventoryItem existingItem = getInventoryItemById(id);
        if (existingItem != null) {
            return;
        }

        // 데이터를 입력한 날짜가 들어가
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormat.format(new Date());

        Random random = new Random();
        // 0 <= 판매량(랜덤값) <= 수량
        int sales = random.nextInt( quantity + 1);
        int stock = quantity - sales;

        db.execSQL("INSERT INTO Inventory (id, name, quantity, sales, stock, date) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{id, name, quantity, sales, stock, date});
    }

    // 원하는 날짜에 데이터를 INSERT
    public void InsertInventoryForDate(int id, String name, int quantity, String desiredDate) {
        SQLiteDatabase db = getWritableDatabase();

        InventoryItem existingItem = getInventoryItemById(id);
        if (existingItem != null) {
            return;
        }

        Random random = new Random();
        // 0 <= 판매량(랜덤값) <= 수량
        int sales = random.nextInt( quantity + 1);
        int stock = quantity - sales;

        db.execSQL("INSERT INTO Inventory (id, name, quantity, sales, stock, date) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{id, name, quantity, sales, stock, desiredDate});

    }

    // UPDATE문
    // random 함수를 이용해 sales (0 <= sales <= quantity)에 랜덤값 대입
    public void UpdateInventory(int id, String name, int additionalQuantity) {
        SQLiteDatabase db = getWritableDatabase();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormat.format(new Date());

        InventoryItem existingItem = getInventoryItemById(id);
        if (existingItem != null) {
            int previousQuantity = existingItem.getQuantity(); // 이전 수량
            int previousSales = existingItem.getSales(); // 이전 판매량
            int quantity = previousQuantity + additionalQuantity; // 이전 수량 + 추가 수량

            // 이전 판매량 <= 판매량(랜덤값) <= 이후 수량
            Random random = new Random();
            int minSales = previousSales + 1;
            int maxSales = quantity - 1; // 최소값
            int sales = random.nextInt(maxSales - minSales +1) + minSales; // 판매량 업데이트
            int stock = quantity - sales; // 재고량 계산

            db.execSQL("UPDATE Inventory SET name=?, quantity=?, sales=?, stock=?, date=? WHERE id=?",
                    new Object[]{name, quantity, sales, stock, date, id});
        }
    }

    // DELETE문
    public void DeleteInventory(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM Inventory WHERE id=?",
                new Object[]{id});
    }

    // "날짜 선택" 버튼 누르면 해당하는 날짜의 데이터들을 보여줌
    public ArrayList<String> getOrderItemsByDate(String date) {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> orderItems = new ArrayList<>();

        // 선택한 날짜에 해당하는 데이터를 검색함
        Cursor cursor = db.rawQuery("SELECT * FROM Inventory WHERE date = ? AND STOCK <= 10 ORDER BY stock ASC", new String[]{date});
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
                String item = "상품명 : " + name + " - "+ " 재고량 : " + stock + " 개";
                orderItems.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return orderItems;
    }


}