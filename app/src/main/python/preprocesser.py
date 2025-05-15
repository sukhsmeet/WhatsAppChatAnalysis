import re
import pandas as pd

def preprocess(data):
    pattern = r"(\d{2}/\d{2}/\d{2}), (\d{1,2}:\d{2}\s[ap]m) - ([^:]+): (.+)"
    matches = re.findall(pattern, data)
    dates = []
    times = []
    senders = []
    messages = []

    for date, time, sender, message in matches:
        dates.append(date)
        times.append(time)
        senders.append(sender)
        messages.append(message)
        
    df = pd.DataFrame(matches, columns=['Date', 'Time', 'Sender', 'Message'])
    
    df['Date'] = pd.to_datetime(df['Date'], format='%d/%m/%y')
    df['Year']=df['Date'].dt.year
    df['Month'] = df['Date'].dt.month_name()
    df['Day'] = df['Date'].dt.day
    df['Time_24hr_dt'] = pd.to_datetime(df['Time'])
    df['Hour'] = df['Time_24hr_dt'].dt.hour
    df['Minute'] = df['Time_24hr_dt'].dt.minute
    df['Day_name'] = df['Date'].dt.day_name()
    
    period = []
    for hour in df[['Day_name','Hour']]['Hour']:
        if hour == 23:
            period.append(str(hour) + "-" + str('00'))
        elif hour ==0:
            period.append(str('00')+"-" + str(hour+1))    
        else:
            period.append(str(hour) + "-" + str(hour+1))
    
    df['Period'] = period
        
    
    return df