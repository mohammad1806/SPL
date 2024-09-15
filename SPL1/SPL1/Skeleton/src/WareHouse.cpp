#include "WareHouse.h"
#include "Volunteer.h"
#include <iostream>
#include <fstream>
#include <string>
#include <algorithm>
#include "Action.h"

WareHouse::WareHouse(const string &configFilePath) : isOpen(), actionsLog(), volunteers(), pendingOrders(), inProcessOrders(), completedOrders(), customers(), customerCounter(0), volunteerCounter(0), orderCounter(0)
{

    std::ifstream inputfile(configFilePath);

    // Check if the file is open successfully
    if (!inputfile.is_open())
    {
        std::cerr << "Error opening file: " << configFilePath << std::endl;
        return;
    }

    // checking the content of each line and use a suitable Action
    std::string line;
    while (getline(inputfile, line))
    {
        if (line.compare(0, 8, "customer") == 0)
        {
            size_t startPos = 9;
            size_t spacePos = line.find(' ', startPos);
            const string customer_name(line.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = line.find(' ', startPos);
            const string customer_type(line.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = line.find(' ', startPos);
            int customer_distance = stoi(line.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = line.find(' ', startPos);
            int max_orders = stoi(line.substr(startPos, spacePos - startPos));
            if (customer_type == "civilian")
            {
                customers.push_back(new CivilianCustomer(customerCounter, customer_name, customer_distance, max_orders));
                increaseCC();
            }
            else
            {
                customers.push_back(new SoldierCustomer(customerCounter, customer_name, customer_distance, max_orders));
                increaseCC();
            }
        }
        if (line.compare(0, 9, "volunteer") == 0)
        {
            size_t startPos = 10;
            size_t spacePos = line.find(' ', startPos);
            const string volunteer_name(line.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = line.find(' ', startPos);
            const string volunteerType(line.substr(startPos, spacePos - startPos));
            if (volunteerType == "collector")
            {
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int coolDown = stoi(line.substr(startPos, spacePos - startPos));
                CollectorVolunteer *v = new CollectorVolunteer(volunteerCounter, volunteer_name, coolDown);
                volunteers.push_back(v);
                volunteerCounter++;
            }
            else if (volunteerType == "limited_collector")
            {
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int coolDown = stoi(line.substr(startPos, spacePos - startPos));
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int maxOrders = stoi(line.substr(startPos, spacePos - startPos));
                LimitedCollectorVolunteer *v = new LimitedCollectorVolunteer(volunteerCounter, volunteer_name, coolDown, maxOrders);
                volunteers.push_back(v);
                volunteerCounter++;
            }
            else if (volunteerType == "driver")
            {
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int maxDistance = stoi(line.substr(startPos, spacePos - startPos));
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int distancePerStep = stoi(line.substr(startPos, spacePos - startPos));
                DriverVolunteer *v = new DriverVolunteer(volunteerCounter, volunteer_name, maxDistance, distancePerStep);
                volunteers.push_back(v);
                volunteerCounter++;
            }
            else if (volunteerType == "limited_driver")
            {
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int maxDistance = stoi(line.substr(startPos, spacePos - startPos));
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int distancePerStep = stoi(line.substr(startPos, spacePos - startPos));
                startPos = spacePos + 1;
                spacePos = line.find(' ', startPos);
                int maxOrders = stoi(line.substr(startPos, spacePos - startPos));
                LimitedDriverVolunteer *v = new LimitedDriverVolunteer(volunteerCounter, volunteer_name, maxDistance, distancePerStep, maxOrders);
                volunteers.push_back(v);
                volunteerCounter++;
            }
        }
    }
}

void WareHouse::start()
{

    this->open();
    WareHouse &w = *this;
    while (isOpen)
    {

        string action;
        getline(std::cin, action);
        size_t startPos = 0;
        size_t spacePos = action.find(' ', startPos);
        if (action.compare(startPos, spacePos, "step") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find('\n', startPos);
            int numOfSteps = stoi(action.substr(startPos, spacePos - startPos));
            SimulateStep S(numOfSteps);
            S.act(w);
        }
        else if (action.compare(startPos, spacePos, "order") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find('\n', startPos);
            int customerId = stoi(action.substr(startPos, spacePos - startPos));
            AddOrder AO(customerId);
            AO.act(w);
        }
        else if (action.compare(startPos, spacePos, "customer") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find(' ', startPos);
            const string customer_name(action.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = action.find(' ', startPos);
            const string customer_type(action.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = action.find(' ', startPos);
            int customer_distance = stoi(action.substr(startPos, spacePos - startPos));
            startPos = spacePos + 1;
            spacePos = action.find(' ', startPos);
            int max_orders = stoi(action.substr(startPos, spacePos - startPos));
            AddCustomer AC(customer_name, customer_type, customer_distance, max_orders);
            AC.act(w);
        }
        else if (action.compare(startPos, spacePos, "orderStatus") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find('\n', startPos);
            int orderId = stoi(action.substr(startPos, spacePos - startPos));
            PrintOrderStatus POS(orderId);
            POS.act(w);
        }
        else if (action.compare(startPos, spacePos, "customerStatus") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find('\n', startPos);
            int customerId = stoi(action.substr(startPos, spacePos - startPos));
            PrintCustomerStatus PCS(customerId);
            PCS.act(w);
        }
        else if (action.compare(startPos, spacePos, "volunteerStatus") == 0)
        {
            startPos = spacePos + 1;
            spacePos = action.find('\n', startPos);
            int volunteerId = stoi(action.substr(startPos, spacePos - startPos));
            PrintVolunteerStatus PVS(volunteerId);
            PVS.act(w);
        }
        else if (action.compare(startPos, spacePos, "log") == 0)
        {
            PrintActionsLog PAL;
            PAL.act(w);
        }
        else if (action.compare(startPos, spacePos, "close") == 0)
        {
            Close c;
            c.act(w);
        }
        else if (action.compare(startPos, spacePos, "backup") == 0)
        {
            BackupWareHouse BW;
            BW.act(w);
        }
        else if (action.compare(startPos, spacePos, "restore") == 0)
        {
            RestoreWareHouse RW;
            RW.act(w);
        }
    }
}

void WareHouse::ptntaction()
{
    if (!actionsLog.empty())
    {
        for (size_t i = 0; i < actionsLog.size(); i++)
        {
            std::cout << actionsLog[i]->toString() << std::endl;
        }
    }
}

void WareHouse::addOrder(Order *order)
{
    this->pendingOrders.push_back(order);
}
void WareHouse::addAction(BaseAction *action)
{
    this->actionsLog.push_back(action);
}
Volunteer &WareHouse::getVolunteer(int volunteerId) const
{
    Volunteer *vol;
    for (Volunteer *v : this->volunteers)
    {
        if (v->getId() == volunteerId)
        {
            vol = v;
        }
    }
    return *vol;
}

int WareHouse::getCustomerCounter()
{
    return this->customerCounter;
}

bool WareHouse::ifExistOrder(int orderId) const
{

    for (Order *o : this->pendingOrders)
    {
        if (o->getId() == orderId)
        {
            return true;
        }
    }

    for (Order *o : this->inProcessOrders)
    {
        if (o->getId() == orderId)
        {
            return true;
        }
    }

    for (Order *o : this->completedOrders)
    {
        if (o->getId() == orderId)
        {
            return true;
        }
    }
    return false;
}
Order &WareHouse::getOrder(int orderId) const
{

    Order *ord;
    for (Order *o : this->pendingOrders)
    {
        if (o->getId() == orderId)
        {
            return *o;
        }
    }

    for (Order *o : this->inProcessOrders)
    {
        if (o->getId() == orderId)
        {
            return *o;
        }
    }

    for (Order *o : this->completedOrders)
    {
        if (o->getId() == orderId)
        {
            ord = o;
        }
    }
    return *ord;
}

vector<Order *> &WareHouse::getCompletedOrders()
{
    return completedOrders;
}
const vector<BaseAction *> &WareHouse::getActions() const
{
    return actionsLog;
}

void WareHouse::close()
{
    isOpen = false;
}

void WareHouse::open()
{
    isOpen = true;
    std::cout << "WareHouse is open!" << std::endl;
}

Customer &WareHouse::getCustomer(int customerId) const
{
    Customer *c;
    for (Customer *cus : this->customers)
    {
        if (cus->getId() == customerId)
        {
            c = cus;
        }
    }
    return *c;
}

vector<Volunteer *> &WareHouse::getVolunteers()
{
    return this->volunteers;
}

vector<Order *> &WareHouse::getPendingOrders()
{
    return this->pendingOrders;
}
vector<Order *> &WareHouse::getinprocess()
{
    return this->inProcessOrders;
}

vector<Customer *> &WareHouse::getCustomers()
{
    return this->customers;
}
bool WareHouse::ifExistVolunteer(int id) const
{

    for (Volunteer *vol : this->volunteers)
    {
        if (vol->getId() == id)
        {
            return true;
        }
    }
    return false;
}
bool WareHouse::ifExistCustomer(int id) const
{

    for (Customer *cus : this->customers)
    {
        if (cus->getId() == id)
        {
            return true;
        }
    }
    return false;
}

void WareHouse::increaseOC()
{
    orderCounter++;
}

void WareHouse::increaseCC()
{
    customerCounter++;
}

int WareHouse::getOrderCounter()
{
    return orderCounter;
}

void WareHouse::removeVolunteer(Volunteer &volunteer)
{
    auto it = std::find(volunteers.begin(), volunteers.end(), &volunteer);
    if (it != volunteers.end())
    {
        volunteers.erase(it);
    }
}

void WareHouse::moveOrderToCompleted(Order &order)
{
    auto it = std::find(inProcessOrders.begin(), inProcessOrders.end(), &order);
    if (it != inProcessOrders.end())
    {
        inProcessOrders.erase(it);
        completedOrders.push_back(&order);
    }
}

void WareHouse::moveOrderToInProcess(Order &order)
{
    auto it = std::find(pendingOrders.begin(), pendingOrders.end(), &order);
    if (it != pendingOrders.end())
    {
        pendingOrders.erase(it);
        inProcessOrders.push_back(&order);
    }
}

void WareHouse::moveOrderToPending(Order &order)
{
    auto it = std::find(inProcessOrders.begin(), inProcessOrders.end(), &order);
    if (it != inProcessOrders.end())
    {
        inProcessOrders.erase(it);
        pendingOrders.push_back(&order);
    }
}

// rule of 5 implementation
// copy constructor
WareHouse::WareHouse(const WareHouse &other) : isOpen(other.isOpen), actionsLog(), volunteers(), pendingOrders(), inProcessOrders(), completedOrders(), customers(), customerCounter(other.customerCounter), volunteerCounter(other.volunteerCounter), orderCounter(other.orderCounter)
{

    for (Customer *cus : other.customers)
    {
        customers.push_back(cus->clone());
    }
    for (Volunteer *vol : other.volunteers)
    {
        volunteers.push_back(vol->clone());
    }
    for (Order *ord : other.pendingOrders)
    {
        pendingOrders.push_back(new Order(*ord));
    }
    for (Order *ord : other.inProcessOrders)
    {
        inProcessOrders.push_back(new Order(*ord));
    }
    for (Order *ord : other.completedOrders)
    {
        completedOrders.push_back(new Order(*ord));
    }
    for (BaseAction *act : other.actionsLog)
    {
        actionsLog.push_back(act->clone());
    }
}

// distructor
WareHouse ::~WareHouse()
{

    for (BaseAction *action : actionsLog)
    {
        if (action != nullptr)
        {
            delete action;
        }
    }

    for (Volunteer *volunteer : volunteers)
    {
        if (volunteer != nullptr)
        {
            delete volunteer;
        }
    }

    for (Order *ordertodel : pendingOrders)
    {
        if (ordertodel != nullptr)
        {
            delete ordertodel;
        }
    }

    for (Order *ordertodel : inProcessOrders)
    {
        if (ordertodel != nullptr)
        {
            delete ordertodel;
        }
    }

    for (Order *ordertodel : completedOrders)
    {
        if (ordertodel != nullptr)
        {
            delete ordertodel;
        }
    }

    for (Customer *custtodel : customers)
    {
        if (custtodel != nullptr)
        {
            delete custtodel;
        }
    }
}

// copy assignment operator
WareHouse &WareHouse ::operator=(const WareHouse &other)
{
    if (&other != this)
    {
        this->isOpen = other.isOpen;

        for (BaseAction *action : this->actionsLog)
        {
            delete action;
        }

        for (Volunteer *volunteer : this->volunteers)
        {
            delete volunteer;
        }

        for (Order *ordertodel : this->pendingOrders)
        {
            delete ordertodel;
        }

        for (Order *ordertodel : this->inProcessOrders)
        {
            delete ordertodel;
        }

        for (Order *ordertodel : this->completedOrders)
        {
            delete ordertodel;
        }

        for (Customer *custtodel : this->customers)
        {
            delete custtodel;
        }

        actionsLog.clear();
        for (BaseAction *action : other.actionsLog)
        {
            this->actionsLog.push_back(action->clone());
        }
        volunteers.clear();
        for (Volunteer *volunteer : other.volunteers)
        {
            this->volunteers.push_back(volunteer->clone());
        }
        pendingOrders.clear();
        for (Order *ordertopush : other.pendingOrders)
        {
            this->pendingOrders.push_back(new Order(*ordertopush));
        }
        inProcessOrders.clear();
        for (Order *ordertopush : other.inProcessOrders)
        {
            this->inProcessOrders.push_back(new Order(*ordertopush));
        }
        completedOrders.clear();
        for (Order *ordertopush : other.completedOrders)
        {
            this->completedOrders.push_back(new Order(*ordertopush));
        }
        customers.clear();
        for (Customer *custtopush : other.customers)
        {
            this->customers.push_back(custtopush->clone());
        }

        this->customerCounter = other.customerCounter;
        this->volunteerCounter = other.volunteerCounter;
        this->orderCounter = other.orderCounter;
    }
    return *this;
}

// move assignment operator
WareHouse &WareHouse::operator=(WareHouse &&other) noexcept
{
    if (this != &other)
    {
        for (BaseAction *action : actionsLog)
        {
            delete action;
        }

        for (Volunteer *volunteer : volunteers)
        {
            delete volunteer;
        }

        for (Order *order : pendingOrders)
        {
            delete order;
        }

        for (Order *order : inProcessOrders)
        {
            delete order;
        }

        for (Order *order : completedOrders)
        {
            delete order;
        }
        completedOrders.clear();

        for (Customer *customer : customers)
        {
            delete customer;
        }

        isOpen = other.isOpen;
        actionsLog = std::move(other.actionsLog);
        volunteers = std::move(other.volunteers);
        pendingOrders = std::move(other.pendingOrders);
        inProcessOrders = std::move(other.inProcessOrders);
        completedOrders = std::move(other.completedOrders);
        customers = std::move(other.customers);
        customerCounter = other.customerCounter;
        volunteerCounter = other.volunteerCounter;
        orderCounter = other.orderCounter;
    }
    return *this;
}

// move constructor
WareHouse::WareHouse(WareHouse &&other)
    : isOpen(other.isOpen),
      actionsLog(std::move(other.actionsLog)),
      volunteers(std::move(other.volunteers)),
      pendingOrders(std::move(other.pendingOrders)),
      inProcessOrders(std::move(other.inProcessOrders)),
      completedOrders(std::move(other.completedOrders)),
      customers(std::move(other.customers)),
      customerCounter(other.customerCounter),
      volunteerCounter(other.volunteerCounter),
      orderCounter(other.orderCounter)
{
}
