#pragma once
#include <string>
#include <vector>
#include "Order.h"
#include "Customer.h"

class BaseAction;
class Volunteer;

// Warehouse responsible for Volunteers, Customers Actions, and Orders.


class WareHouse {

    public:
        WareHouse(const string &configFilePath);
        void start();
        void addOrder(Order* order);
        void addAction(BaseAction* action);
        Customer &getCustomer(int customerId) const;
        Volunteer &getVolunteer(int volunteerId) const;
        Order &getOrder(int orderId) const;
        const vector<BaseAction*> &getActions() const;
        void close();
        void open();

        //methods i add
        vector<Order*> &getPendingOrders();
        vector<Volunteer*> &getVolunteers();
        vector<Order*> &getinprocess();
        vector<Order*> &getCompletedOrders();
        vector<Customer*> &getCustomers();
        int getCustomerCounter();
        bool ifExistCustomer(int id) const;
        bool ifExistVolunteer(int id) const;
        bool ifExistOrder(int id) const;
        int getOrderCounter();
        void increaseOC();
        void increaseCC();
        void ptntaction();
        void removeVolunteer(Volunteer& volunteer);
        void moveOrderToCompleted(Order& order);
        void moveOrderToInProcess(Order& order);
        void moveOrderToPending(Order& order);

        // rule of five
        ~WareHouse();
        WareHouse& operator=(const WareHouse &WareHouse);
        WareHouse(const WareHouse &WareHouse);
        WareHouse& operator=(WareHouse&& other) noexcept;
        WareHouse(WareHouse&& other);

        


    private:
        bool isOpen;
        vector<BaseAction*> actionsLog;
        vector<Volunteer*> volunteers;
        vector<Order*> pendingOrders;
        vector<Order*> inProcessOrders;
        vector<Order*> completedOrders;
        vector<Customer*> customers;
        int customerCounter; //For assigning unique customer IDs
        int volunteerCounter; //For assigning unique volunteer IDs

        //feilds i add
        int orderCounter;
};