#include "Order.h"
#include <iostream>
Order::Order(int id, int customerId, int distance):
id(id) , customerId(customerId) , distance(distance) , status(OrderStatus::PENDING) , collectorId(NO_VOLUNTEER) , driverId(NO_VOLUNTEER){}

int Order::getId() const{
    return id;
}

int Order::getCustomerId() const{
    return customerId;
}

void Order::setStatus(OrderStatus status){
    this->status = status;
}

void Order::setCollectorId(int collectorId){
    this->collectorId = collectorId;
}

void Order::setDriverId(int driverId){
    this->driverId = driverId;
}

int Order::getCollectorId() const{
    return collectorId;
}

int Order::getDriverId() const{
    return driverId;
}

OrderStatus Order::getStatus() const{
    return status;
}

int Order::getDistance() const{
    return distance;
}
string Order::orderStatusToString() const{
    string s;
        switch (status) {
            case OrderStatus::PENDING:
                s = "Pending";
                break;
            case OrderStatus::COLLECTING:
                s = "Collecting";
                break;
            case OrderStatus::DELIVERING:
                s = "Delivering";
                break;
            case OrderStatus::COMPLETED:
                s = "Completed";
                break;
        }
        return s;
}

const string Order::toString() const{
    string col;
    if(collectorId != NO_VOLUNTEER){
        col = std::to_string(collectorId);
    }else{
        col = "None";
    }
    string dri;
    if(driverId != NO_VOLUNTEER){
        dri = std::to_string(driverId);
    }else{
        dri = "None";
    }  
    return "OrderId: " + std::to_string(id) + 
           "\nOrderStatus: " + orderStatusToString() +
           "\nCustomerId: " + std::to_string(customerId) +
           "\nCollector: " + col +
           "\nDriver: " + dri;
}

