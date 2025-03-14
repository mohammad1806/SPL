#include "Action.h"
#include "Volunteer.h"
#include "WareHouse.h"
#include <iostream>
#include <algorithm>

extern WareHouse *backup;

// BaseAction
BaseAction::BaseAction() : errorMsg(), status() {}
ActionStatus BaseAction::getStatus() const
{
    std::cout << statusToString() << std::endl;
    return status;
}

void BaseAction::complete()
{
    this->status = ActionStatus::COMPLETED;
}

void BaseAction::error(string errorMsg)
{
    this->status = ActionStatus::ERROR;
    this->errorMsg = errorMsg;
}

string BaseAction::getErrorMsg() const
{
    return this->errorMsg;
}

string BaseAction::statusToString() const
{
    string s;
    switch (status)
    {
    case ActionStatus::COMPLETED:
        s = "COMPLETED";
        break;
    case ActionStatus::ERROR:
        s = "ERORR";
        break;
    }
    return s;
}

//<--------------------------------------------------------------------------------------------->
// SimulateStep
SimulateStep::SimulateStep(int numOfSteps) : numOfSteps(numOfSteps) {}

void SimulateStep::act(WareHouse &wareHouse)
{

    for (int i = 0; i < numOfSteps; i++)
    {
        for (int i = 0; i < (int)wareHouse.getPendingOrders().size(); i++)
        {

            Order *o = wareHouse.getPendingOrders()[i];

            for (Volunteer *v : wareHouse.getVolunteers())
            {

                if (v->canTakeOrder(*o))
                {
                    if (o->getStatus() == OrderStatus::PENDING)
                    {
                        v->acceptOrder(*o);
                        o->setStatus(OrderStatus::COLLECTING);
                        o->setCollectorId(v->getId());
                        wareHouse.moveOrderToInProcess(*o);
                        i--;
                    }
                    else if (o->getStatus() == OrderStatus::COLLECTING)
                    {
                        v->acceptOrder(*o);
                        o->setStatus(OrderStatus::DELIVERING);
                        o->setDriverId(v->getId());
                        wareHouse.moveOrderToInProcess(*o);
                        i--;
                    }
                    break;
                }
            }
        }

        for (int j = 0 ; j < (int)wareHouse.getVolunteers().size() ; j++)
        {
            Volunteer* vol = wareHouse.getVolunteers()[j];

            if (vol->isBusy())
            {
                vol->step();

                for (int i = 0; i < (int)wareHouse.getinprocess().size(); i++)
                {
                    Order *ord = wareHouse.getinprocess()[i];
                    if (vol->getCompletedOrderId() == ord->getId())
                    {
                        if (ord->getStatus() == OrderStatus::COLLECTING)
                        {
                            wareHouse.moveOrderToPending(*ord);
                            i--;
                        }
                        else if (ord->getStatus() == OrderStatus::DELIVERING)
                        {
                            ord->setStatus(OrderStatus::COMPLETED);
                            wareHouse.moveOrderToCompleted(*ord);
                            i--;
                        }
                    }
                }
            }

            if (!vol->hasOrdersLeft() && !vol->isBusy())
            {
                wareHouse.removeVolunteer(*vol);
                j--;
                delete vol;
            }
        }
    }
    this->complete();
    wareHouse.addAction(this->clone());
}

string SimulateStep::toString() const
{
    return "simulateStep " + std::to_string(numOfSteps) + " " + statusToString();
}

SimulateStep *SimulateStep::clone() const
{
    return new SimulateStep(*this);
}

//<--------------------------------------------------------------------------------------------->
// AddOrder
AddOrder::AddOrder(int id) : customerId(id) {}

void AddOrder::act(WareHouse &wareHouse)
{
    if (!wareHouse.ifExistCustomer(customerId) || !wareHouse.getCustomer(customerId).canMakeOrder())
    {
        this->error("ERROR: Cannot place this Order");
        std::cout << this->getErrorMsg() << std::endl;
        wareHouse.addAction(this->clone());
        return;
    }
    Order *toAdd = new Order(wareHouse.getOrderCounter(), customerId, wareHouse.getCustomer(customerId).getCustomerDistance());
    wareHouse.getCustomer(customerId).addOrder(toAdd->getId());
    wareHouse.addOrder(toAdd);
    this->complete();
    wareHouse.addAction(this->clone());
    wareHouse.increaseOC();
}

string AddOrder::toString() const
{
    return "order " + std::to_string(customerId) + " " + statusToString();
}

AddOrder *AddOrder::clone() const
{
    return new AddOrder(*this);
}

//<--------------------------------------------------------------------------------------------->
// AddCustomer
AddCustomer::AddCustomer(const string &customerName, const string &custType, int distance, int maxOrders) : customerName(customerName), customerType(AddCustomer::convrtToCT(custType)), distance(distance), maxOrders(maxOrders) {}

void AddCustomer::act(WareHouse &wareHouse)
{
    if (customerType == CustomerType::Civilian)
    {
        wareHouse.getCustomers().push_back(new CivilianCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders));
    }
    else
    {
        wareHouse.getCustomers().push_back(new SoldierCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders));
    }
    this->complete();
    wareHouse.addAction(this->clone());
    wareHouse.increaseCC();
}

CustomerType AddCustomer::getCustType() const
{
    return customerType;
}

string AddCustomer::CustTypeToString() const
{
    string s = "";
    if (customerType == CustomerType::Civilian)
    {
        s = "civilian";
    }
    else
    {
        s = "soldier";
    }
    return s;
}

string AddCustomer::toString() const
{
    return "customer " + customerName + " " +
           CustTypeToString() + " " +
           std::to_string(distance) + " " +
           std::to_string(maxOrders) + " " + statusToString();
}

CustomerType AddCustomer::convrtToCT(const string &customerType)
{

    CustomerType c = CustomerType::Civilian;
    if (customerType == "civilian")
    {
        return c;
    }
    else if (customerType == "soldier")
    {
        c = CustomerType::Soldier;
    }
    return c;
}

AddCustomer *AddCustomer::clone() const
{
    return new AddCustomer(*this);
}

//<--------------------------------------------------------------------------------------------->
// PrintOrderStatus
PrintOrderStatus::PrintOrderStatus(int id) : orderId(id) {}
void PrintOrderStatus::act(WareHouse &wareHouse)
{
    if (!wareHouse.ifExistOrder(orderId))
    {
        this->error("ERROR: Order doesn't exist");
        std::cout << getErrorMsg() << std::endl;
        wareHouse.addAction(this->clone());
        return;
    }
    std::cout << wareHouse.getOrder(orderId).toString() << std::endl;
    this->complete();
    wareHouse.addAction(this->clone());
}
string PrintOrderStatus::toString() const
{
    return "orderStatus " + std::to_string(orderId) + " " + statusToString();
}
PrintOrderStatus *PrintOrderStatus::clone() const
{
    return new PrintOrderStatus(*this);
}

//<--------------------------------------------------------------------------------------------->
// PrintCustomerStatus
PrintCustomerStatus::PrintCustomerStatus(int customerId) : customerId(customerId) {}

void PrintCustomerStatus::act(WareHouse &wareHouse)
{
    if (!wareHouse.ifExistCustomer(customerId))
    {
        this->error("ERROR: Customer doesn't exist");
        std::cout << getErrorMsg() << std::endl;
        wareHouse.addAction(this->clone());
        return;
    }

    string s = "";
    for (int i : wareHouse.getCustomer(customerId).getOrdersIds())
    {
        s = s + "\nOrderID: " + std::to_string(wareHouse.getOrder(i).getId()) + "\nOrderStatus: " + wareHouse.getOrder(i).orderStatusToString();
    }
    std::cout << "CustomerID: " + std::to_string(customerId) + s + "\nnumOrdersLeft: " +
                     std::to_string(wareHouse.getCustomer(customerId).getMaxOrders() - wareHouse.getCustomer(customerId).getNumOrders())
              << std::endl;
    this->complete();
    wareHouse.addAction(this->clone());
}

string PrintCustomerStatus::toString() const
{
    return "CustomerStatus " + std::to_string(customerId) + " " + statusToString();
}

PrintCustomerStatus *PrintCustomerStatus::clone() const
{
    return new PrintCustomerStatus(*this);
}

//<--------------------------------------------------------------------------------------------->
// PrintVolunteerStatus
PrintVolunteerStatus::PrintVolunteerStatus(int id) : volunteerId(id) {}

void PrintVolunteerStatus::act(WareHouse &wareHouse)
{
    if (!wareHouse.ifExistVolunteer(volunteerId))
    {
        this->error("ERROR: Volunteer doesn't exist");
        std::cout << getErrorMsg() << std::endl;
        wareHouse.addAction(this->clone());
        return;
    }

    std::cout << wareHouse.getVolunteer(volunteerId).toString() << std::endl;
    this->complete();
    wareHouse.addAction(this->clone());
}

string PrintVolunteerStatus::toString() const
{
    return "VolunteerStatus " + std::to_string(volunteerId) + " " + statusToString();
}

PrintVolunteerStatus *PrintVolunteerStatus::clone() const
{
    return new PrintVolunteerStatus(*this);
}

//<--------------------------------------------------------------------------------------------->
// PrintActionLogs
PrintActionsLog::PrintActionsLog() {}

void PrintActionsLog::act(WareHouse &wareHouse)
{

    wareHouse.ptntaction();

    this->complete();
    wareHouse.addAction(this->clone());
}

string PrintActionsLog::toString() const
{
    return "log " + statusToString();
}

PrintActionsLog *PrintActionsLog::clone() const
{
    return new PrintActionsLog(*this);
}

//<--------------------------------------------------------------------------------------------->
// close
Close::Close() {}
void Close::act(WareHouse &wareHouse)
{

    for (Order *ord : wareHouse.getPendingOrders())
    {
        std::cout << "OrderID: " + std::to_string(ord->getId()) << ",CustomerID: " + std::to_string(ord->getCustomerId()) << ",OrderStatus: " + ord->orderStatusToString() << std::endl;
    }

    for (Order *ord : wareHouse.getinprocess())
    {
        std::cout << "OrderID: " + std::to_string(ord->getId()) << ",CustomerID: " + std::to_string(ord->getCustomerId()) << ",OrderStatus: " + ord->orderStatusToString() << std::endl;
    }

    for (Order *ord : wareHouse.getCompletedOrders())
    {
        std::cout << "OrderID: " + std::to_string(ord->getId()) << ",CustomerID: " + std::to_string(ord->getCustomerId()) << ",OrderStatus: " + ord->orderStatusToString() << std::endl;
    }

    this->complete();
    wareHouse.addAction(this->clone());
    wareHouse.close();
}
string Close::toString() const
{
    return "close " + statusToString();
}

Close *Close::clone() const
{
    return new Close(*this);
}

//<--------------------------------------------------------------------------------------------->
// BackupWareHouse
BackupWareHouse::BackupWareHouse() {}
void BackupWareHouse::act(WareHouse &wareHouse)
{
    if( backup != nullptr){
        delete backup;
    }
    complete();
    wareHouse.addAction(this->clone());
    backup = new WareHouse(wareHouse);
}

string BackupWareHouse::toString() const
{
    return "backup " + statusToString();
}

BackupWareHouse *BackupWareHouse::clone() const
{
    return new BackupWareHouse(*this);
}

//<--------------------------------------------------------------------------------------------->
// restoreWareHouse
RestoreWareHouse::RestoreWareHouse() {}
void RestoreWareHouse::act(WareHouse &wareHouse)
{
    if (backup != nullptr)
    {
        wareHouse.operator=(*backup);
        complete();
    }
    else
    {
        error("ERROR: No backup available");
        std::cout << this->getErrorMsg() << std::endl;
    }
    wareHouse.addAction(this->clone());
}

RestoreWareHouse *RestoreWareHouse::clone() const
{
    return new RestoreWareHouse(*this);
}
string RestoreWareHouse::toString() const
{
    return "restore " + statusToString();
}